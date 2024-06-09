package core.main;
import org.lwjgl.*;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.*;
import java.util.Set;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

import core.Internal;
import core.Structures.*;

public class JavaVK {

    public static VulkanContext context;
    public static MemoryStack stack;
    public static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
                                                              .collect(java.util.stream.Collectors.toSet());

    public void initialize() {

        context = new VulkanContext();

        if (!glfwInit()) {
            throw new RuntimeException("Couldn't initialize GLFW");
        }
        if (!glfwVulkanSupported()) {
            throw new RuntimeException("Vulkan not supported");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);



        context.window = glfwCreateWindow(1200, 800, "Vulkan Window", NULL, NULL);
        if (context.window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
        if (requiredExtensions == null) {
            throw new RuntimeException("Failed to find required GLFW extensions");
        }
        stack = MemoryStack.stackPush();

        VkApplicationInfo applicationInfo = VkApplicationInfo.calloc(stack)
            .sType$Default()
            .pApplicationName(      stack.UTF8("Java Graphics"))
            .pEngineName(           stack.UTF8("Java Graphics"))
            .applicationVersion(    VK_MAKE_VERSION(1, 0, 0))
            .engineVersion(         VK_MAKE_VERSION(1, 0, 0))
            .apiVersion(            VK_API_VERSION_1_0);

        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
            .sType$Default()
            .pApplicationInfo(applicationInfo)
            .ppEnabledExtensionNames(requiredExtensions);


        PointerBuffer pInstance = stack.mallocPointer(1);
        if (vkCreateInstance(createInfo, null, pInstance) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create VkInstance");
        }
        context.instance = new VkInstance(pInstance.get(0), createInfo);
            


        LongBuffer pSurface = stack.mallocLong(1);
        if (glfwCreateWindowSurface(context.instance, context.window, null, pSurface) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create window surface");
        }
        context.surface = pSurface.get(0);



        PointerBuffer pPhysicalDevices;
        IntBuffer pPhysicalDeviceCount = stack.ints(0);

        vkEnumeratePhysicalDevices(context.instance, pPhysicalDeviceCount, null);
        pPhysicalDevices = MemoryUtil.memAllocPointer(pPhysicalDeviceCount.get(0));

        vkEnumeratePhysicalDevices(context.instance, pPhysicalDeviceCount, pPhysicalDevices);
        context.physicalDevice = Internal.findPhysicalDevice(pPhysicalDevices);

        Internal.createDevice();
        Internal.createSwapchain();

        mainloop();
    }

    public void mainloop() {

        while (!glfwWindowShouldClose(context.window)) {
            glfwPollEvents();
        }
    }
}
