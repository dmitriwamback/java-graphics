package core;

import core.Structures.*;
import core.main.JavaVK;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class Internal {

    public static QueueFamily family;

    public static List<VkCommandBuffer> commandBuffers;
    public static List<Long>            imageSemaphores, renderSemaphores, inFlightFence;
    long                                commandPool;

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static void initialize() {
        //family = queryQueueFamily();
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    
    public static QueueFamily queryQueueFamily(VkPhysicalDevice physicalDevice) {
        QueueFamily family = new QueueFamily();

        IntBuffer pFamilyCount = MemoryUtil.memAllocInt(1);
        int familyCount;
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pFamilyCount, null);        
        familyCount = pFamilyCount.get(0);

        VkQueueFamilyProperties.Buffer queueFamilyProperties = VkQueueFamilyProperties.calloc(familyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pFamilyCount, queueFamilyProperties);

        int graphicsQueueFamilyIndex = -1;

        for (int i = 0; i < familyCount; i++) {
            if ((queueFamilyProperties.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                family.graphicsFamily = i;
                graphicsQueueFamilyIndex = i;
            }
                
            IntBuffer present = MemoryUtil.memAllocInt(1);
            vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, graphicsQueueFamilyIndex, JavaVK.context.surface, present);
            if (present.get(0) == 1) family.presentFamily = graphicsQueueFamilyIndex;

        }
        if (graphicsQueueFamilyIndex == -1) {
            throw new RuntimeException("No graphics queue family found");
        }
        return family;
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static void createSynchronizedObjects() {
        
        Internal.imageSemaphores    = new ArrayList<Long>();
        Internal.renderSemaphores   = new ArrayList<Long>();
        Internal.inFlightFence      = new ArrayList<Long>();
        Internal.commandBuffers     = new ArrayList<VkCommandBuffer>();

        MemoryStack stack = MemoryStack.stackPush();

        VkCommandPoolCreateInfo poolCreateInfo = VkCommandPoolCreateInfo.calloc(stack)
            .sType$Default()
            .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
            .queueFamilyIndex(family.graphicsFamily);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static void querySwapchainSupport() {
        
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static SwapchainDetails querySwapchainDetails(VkPhysicalDevice pDevice) {

        SwapchainDetails details = new SwapchainDetails();
        details.capabilitiesKHR = VkSurfaceCapabilitiesKHR.calloc(JavaVK.stack);

        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(pDevice, JavaVK.context.surface, details.capabilitiesKHR);

        IntBuffer pFormatCount = MemoryUtil.memAllocInt(1),
                  pPresentModeCount = MemoryUtil.memAllocInt(1);

        vkGetPhysicalDeviceSurfaceFormatsKHR(pDevice, JavaVK.context.surface, pFormatCount, null);
        vkGetPhysicalDeviceSurfacePresentModesKHR(pDevice, JavaVK.context.surface, pPresentModeCount, null);

        if (pFormatCount.get(0) != 0) {
            details.formatKHR = VkSurfaceFormatKHR.calloc(pFormatCount.get(0));
            vkGetPhysicalDeviceSurfaceFormatsKHR(
                pDevice, 
                JavaVK.context.surface, 
                pFormatCount, 
                details.formatKHR);
        }

        if (pPresentModeCount.get(0) != 0) {
            details.presentModeKHR = MemoryUtil.memAllocInt(pPresentModeCount.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(
                pDevice, 
                JavaVK.context.surface, 
                pPresentModeCount, 
                details.presentModeKHR);
        }

        return details;
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    private static boolean checkDeviceExtensions(VkPhysicalDevice pDevice) {

        IntBuffer pExtensionCount = MemoryUtil.memAllocInt(1);
        VkExtensionProperties.Buffer availableExtensions;
        vkEnumerateDeviceExtensionProperties(pDevice, (String)null, pExtensionCount, null);

        availableExtensions = VkExtensionProperties.malloc(pExtensionCount.get(0), JavaVK.stack);
        vkEnumerateDeviceExtensionProperties(pDevice, (String)null, pExtensionCount, availableExtensions);

        return availableExtensions.stream()
                .map(VkExtensionProperties::extensionNameString)
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(JavaVK.DEVICE_EXTENSIONS);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    private static boolean isPhysicalDeviceSuitable(VkPhysicalDevice pDevice) {

        VkPhysicalDeviceProperties properties   = VkPhysicalDeviceProperties.calloc();
        VkPhysicalDeviceFeatures features       = VkPhysicalDeviceFeatures.calloc();

        vkGetPhysicalDeviceProperties(pDevice, properties);
        vkGetPhysicalDeviceFeatures(pDevice, features);

        SwapchainDetails swapchainDetails = querySwapchainDetails(pDevice);
        boolean isAdequate = false;
        if (swapchainDetails.formatKHR != null && swapchainDetails.presentModeKHR != null) isAdequate = true;

        QueueFamily family = queryQueueFamily(pDevice);
        return family.graphicsFamily != -1 && checkDeviceExtensions(pDevice) && isAdequate;
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static VkPhysicalDevice findPhysicalDevice(PointerBuffer devices) {

        VkPhysicalDevice pDevice = new VkPhysicalDevice(devices.get(0), JavaVK.context.instance);
        for (int i = 0; i < devices.capacity(); i++) {

            pDevice = new VkPhysicalDevice(devices.get(i), JavaVK.context.instance);
            isPhysicalDeviceSuitable(pDevice);
        }

        return pDevice;
    }
}
