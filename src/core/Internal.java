package core;

import core.Structures.*;
import core.main.JavaVK;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class Internal {

    public static QueueFamily family;

    public static List<VkCommandBuffer> commandBuffers;
    public static LongBuffer            imageSemaphores, renderSemaphores, inFlightFence;
    public static LongBuffer            commandPool;

    public static int                   IN_FLIGHT_FRAMES = 2;

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
        
        Internal.imageSemaphores    = MemoryUtil.memAllocLong(IN_FLIGHT_FRAMES);
        Internal.renderSemaphores   = MemoryUtil.memAllocLong(IN_FLIGHT_FRAMES);
        Internal.inFlightFence      = MemoryUtil.memAllocLong(IN_FLIGHT_FRAMES);
        Internal.commandBuffers     = new ArrayList<VkCommandBuffer>();

        VkCommandPoolCreateInfo poolCreateInfo = VkCommandPoolCreateInfo.calloc(JavaVK.stack)
            .sType$Default()
            .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
            .queueFamilyIndex(family.graphicsFamily);

        if (vkCreateCommandPool(JavaVK.context.device, poolCreateInfo, null, commandPool) != VK_SUCCESS) throw new RuntimeException("");

        VkCommandBufferAllocateInfo commandBufferAlloc = VkCommandBufferAllocateInfo.calloc(JavaVK.stack)
            .sType$Default()
            .commandBufferCount(IN_FLIGHT_FRAMES)
            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            .commandPool(commandPool.get(0));

        PointerBuffer pCommandBuffers = MemoryUtil.memAllocPointer(IN_FLIGHT_FRAMES);
        
        if (vkAllocateCommandBuffers(JavaVK.context.device, commandBufferAlloc, pCommandBuffers) != VK_SUCCESS) throw new RuntimeException("");
        for (int i = 0; i < IN_FLIGHT_FRAMES; i++) {
            commandBuffers.set(i, new VkCommandBuffer(pCommandBuffers.get(i), JavaVK.context.device));
        }

        VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc(JavaVK.stack)
            .sType$Default()
            .flags(VK_FENCE_CREATE_SIGNALED_BIT);
        
        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(JavaVK.stack)
            .sType$Default();

        for (int i = 0; i < IN_FLIGHT_FRAMES; i++) {

            if (vkCreateSemaphore(JavaVK.context.device, semaphoreCreateInfo, null, imageSemaphores)  != VK_SUCCESS ||
                vkCreateSemaphore(JavaVK.context.device, semaphoreCreateInfo, null, renderSemaphores) != VK_SUCCESS || 
                vkCreateFence(JavaVK.context.device, fenceCreateInfo, null, inFlightFence) != VK_SUCCESS)
            {
                throw new RuntimeException("");
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static VkSurfaceFormatKHR chooseSwapchainSurface(List<VkSurfaceFormatKHR> availableFormats) {

        for (VkSurfaceFormatKHR format : availableFormats) {
            if (format.format() == VK_FORMAT_B8G8R8_SRGB && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) return format;
        }

        return availableFormats.get(0);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static int chooseSwapchainPresentMode(IntBuffer availablePresentModes) {

        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) return availablePresentModes.get(i);
        }

        return availablePresentModes.get(0);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {

        if (capabilities.currentExtent().width() != Integer.MAX_VALUE) return capabilities.currentExtent();

        IntBuffer pWidth  = MemoryUtil.memAllocInt(1), 
                  pHeight = MemoryUtil.memAllocInt(1);
        GLFW.glfwGetFramebufferSize(JavaVK.context.window, pWidth, pHeight);

        int width  = pWidth.get(0), 
            height = pHeight.get(0);

        if (width < capabilities.minImageExtent().width()) width = capabilities.minImageExtent().width();
        if (width > capabilities.maxImageExtent().width()) width = capabilities.maxImageExtent().width();

        if (height < capabilities.minImageExtent().height()) height = capabilities.minImageExtent().height();
        if (height > capabilities.maxImageExtent().height()) height = capabilities.maxImageExtent().height();

        VkExtent2D extent = VkExtent2D.calloc(JavaVK.stack);
        extent.set(width, height);

        return extent;
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

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static void createSwapchain() {

        SwapchainDetails details    = querySwapchainDetails(JavaVK.context.physicalDevice);
        VkSurfaceFormatKHR sformat  = details.formatKHR.get();
        int pMode                   = chooseSwapchainPresentMode(details.presentModeKHR);
        VkExtent2D extent           = chooseSwapExtent(details.capabilitiesKHR);

        JavaVK.context.extent = extent;
        JavaVK.context.format = sformat.format();

        int imageCount = details.capabilitiesKHR.minImageCount() + 1;
        if (details.capabilitiesKHR.maxImageCount() > 0 && imageCount > details.capabilitiesKHR.minImageCount()) imageCount = details.capabilitiesKHR.maxImageCount();

        VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(JavaVK.stack)
            .sType$Default()
            .surface(JavaVK.context.surface)
            .minImageCount(imageCount)
            .imageFormat(sformat.format())
            .imageColorSpace(sformat.colorSpace())
            .imageExtent(extent)
            .imageArrayLayers(1)
            .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
            .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .preTransform(details.capabilitiesKHR.currentTransform())
            .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            .presentMode(pMode)
            .clipped(true)
            .oldSwapchain(VK_NULL_HANDLE);

        LongBuffer pSwapchain = JavaVK.stack.longs(VK_NULL_HANDLE);;
        
        //if (KHRSwapchain.vkCreateSwapchainKHR(JavaVK.context.device, swapchainCreateInfo, null, pSwapchain) != VK_SUCCESS) {
        //    throw new RuntimeException("Failed to create swapchain");
        //}

        JavaVK.context.swapchain = pSwapchain.get(0);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------//

    public static void createDevice() {

        QueueFamily family = queryQueueFamily(JavaVK.context.physicalDevice);
        VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(family.presentFamily - family.graphicsFamily + 1);

        for (int i = family.graphicsFamily; i <= family.presentFamily; i++) {

            FloatBuffer queuePriority = MemoryUtil.memAllocFloat(1);
            queuePriority.put(1.0f);

            VkDeviceQueueCreateInfo queueCreateInfo = VkDeviceQueueCreateInfo.calloc(JavaVK.stack)
                .sType$Default()
                .queueFamilyIndex(i)
                .pQueuePriorities(queuePriority);
            queueCreateInfos.put(queueCreateInfo);
        }

        String[] extensions = {
            KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME,
            "VK_KHR_portability_subset"
        };

        PointerBuffer pDeviceExtensionNames = MemoryUtil.memAllocPointer(2);
        PointerBuffer pDeviceExtensionNameCount = MemoryUtil.memAllocPointer(1);

        for (String extension : extensions) {
            ByteBuffer extensionBuffer = JavaVK.stack.UTF8(extension);
            pDeviceExtensionNames.put(extensionBuffer);
        }

        pDeviceExtensionNameCount.put(2);

        VkPhysicalDeviceFeatures physicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc(JavaVK.stack);
        VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(JavaVK.stack)
            .set(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO, 
                 VK_NULL_HANDLE, 
                 0, 
                 queueCreateInfos, pDeviceExtensionNames, pDeviceExtensionNameCount, physicalDeviceFeatures);
        
        PointerBuffer pDevice = MemoryUtil.memAllocPointer(1);

        if (vkCreateDevice(JavaVK.context.physicalDevice, deviceCreateInfo, null, pDevice) != VK_SUCCESS) {
            throw new RuntimeException();
        }

        JavaVK.context.device = new VkDevice(pDevice.get(0), JavaVK.context.physicalDevice, deviceCreateInfo);
    }
}
