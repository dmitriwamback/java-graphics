package core;

import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.vulkan.*;

public class Structures {

    public static int FRAMES = 2;
    
    public static class VulkanContext {
        public VkInstance                               instance;
        public VkPhysicalDevice                         physicalDevice;
        public VkDevice                                 device;

        public VkQueue                                  graphicsQueue, 
                                                        presentQueue;

        public long                                     surface;
        public long                                     swapchain;
        public VkExtent2D                               extent;
        public VkDebugUtilsMessengerCallbackEXT         debug;
        public int                                      format;
        public long                                     window;
    }

    public static class QueueFamily {
        int graphicsFamily;
        int presentFamily;

        public QueueFamily() {
            graphicsFamily = -1;
            presentFamily = -1;
        }
    }

    public static class SwapchainDetails {
        public VkSurfaceCapabilitiesKHR                 capabilitiesKHR;
        public VkSurfaceFormatKHR.Buffer                formatKHR;
        public IntBuffer                                presentModeKHR;
    }

    public static class PipelineCreateInformation {
        public VkPipelineDynamicStateCreateInfo        dynamicStateCreateInfo;
        public VkPipelineVertexInputStateCreateInfo    vertexInputCreateInfo;
        public VkPipelineInputAssemblyStateCreateInfo  inputAssemblyStateCreateInfo;
        public VkPipelineViewportStateCreateInfo       viewportStateCreateInfo;
        public VkPipelineRasterizationStateCreateInfo  rasterizationStateCreateInfo;
        public VkPipelineColorBlendStateCreateInfo     colorBlendStateCreateInfo;
        
        public long imageIndex;
    }

    public static class PipelineDetails {
        public long                                     pipelineLayout;
        public long                                     renderpass;
        public VkRect2D                                 scissor;
        public VkDescriptorSetLayoutBinding             uniformLayoutBinding;
        public VkDescriptorSetLayoutCreateInfo          uniformBufferCreateInfo;
        public int                                      setLayoutDescriptor;
    }

    public static class UniformBufferMemory {
        public List<Long>                               uniformBuffers;
        public List<Long>                               uniformBufferMemory;
        public List<Long>                               mappedMemory;
    }

    public static class VertexBufferMemory {
        long vertexBuffer,
             indexBuffer,
             vertexBufferMemory,
             indexBufferMemory;
        boolean isIndexed;
    }

    public static class Pipeline {
        long                        pipeline;
        PipelineDetails             pipelineDetails;
        PipelineCreateInformation   pipelineCreateInformation;
        UniformBufferMemory         uniformBufferMemory;
        VertexBufferMemory          vertexBufferMemory;
    }
}
