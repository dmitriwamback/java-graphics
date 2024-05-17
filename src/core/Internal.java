package core;

import core.Structures.*;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public class Internal {

    public static QueueFamily family;

    public static List<VkCommandBuffer> commandBuffers;
    public static List<Long>            imageSemaphores, renderSemaphores, inFlightFence;
    long                                commandPool;
    
    //QueueFamily QueryQueueFamily() {
    //
    //}

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
        
        //LongBuffer buffer = MemoryUtil.memLongBuffer(0, 0)
        //if (VK10.vkCreateCommandPool(JavaVK.context.device, poolCreateInfo, null, ))
    }

    public static void querySwapchainSupport() {
        
    }

    public static void initialize() {
        family = new QueueFamily();
    }
}
