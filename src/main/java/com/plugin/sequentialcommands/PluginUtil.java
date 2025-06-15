package com.plugin.sequentialcommands;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class PluginUtil {

    public static String getPasswordFromKeyStorage(String path, PluginStepContext context, INodeEntry node)
            throws NodeStepException {
        try {
            // リソースが存在するかを確認
            var resource = context.getExecutionContext()
                    .getStorageTree()
                    .getResource(path);

            if (resource == null || resource.getContents() == null) {
                throw new NodeStepException(
                        "Key Storage path not found: " + path,
                        Sequentialcommands.Reason.KeyStorage,
                        node.getNodename()
                );
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resource.getContents().writeContent(byteArrayOutputStream);

            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new NodeStepException(
                    "Error accessing Key Storage at path '" + path + "': " + e.getMessage(),
                    e,
                    Sequentialcommands.Reason.KeyStorage,
                    node.getNodename()
            );
        }
    }
}
