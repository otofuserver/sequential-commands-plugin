package com.plugin.sequentialcommands;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.google.gson.*;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

/*
Created by Jake Cohen on 3/20/2022
This Rundeck plugin is used to send sequential commands via SSH.
 */
@Plugin(service=ServiceNameConstants.WorkflowNodeStep,name="sequential-commands")
@PluginDescription(title="sequential-commands", description="Send multiple commands in a single SSH Session. Particularly useful for network devices.")
public class Sequentialcommands implements NodeStepPlugin, Describable{

    public static final String SERVICE_PROVIDER_NAME = "sequential-commands";

   /**
     * Overriding this method gives the plugin a chance to take part in building the {@link
     * com.dtolabs.rundeck.core.plugins.configuration.Description} presented by this plugin.  This subclass can use the
     * {@link DescriptionBuilder} to modify all aspects of the description, add or remove properties, etc.
     */
   @Override
   public Description getDescription() {
        return DescriptionBuilder.builder()
            .name(SERVICE_PROVIDER_NAME)
            .title("Sequential SSH Commands")
            .description("Send multiple commands in a single SSH Session. Particularly useful for network devices.")
            .property(PropertyBuilder.builder()
                    .string("custom")
                    .title("Commands")
                    .description("Use \"Add Custom Field\" to add commands you want to send.\n" +
                            "The Label is a user-friendly descriptor for the command.\n" +
                            "The key is a unique identifier of your choosing for that command.\n" +
                            "Once the custom field is created, input your command into the field's textarea.")
                    .renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, "DYNAMIC_FORM")
                    .build()
            )
            .property(PropertyBuilder.builder()
                    .booleanType("strictHostKey")
                    .title("Strict Host Key Checking")
                    .description("If selected, require remote-host SSH key to be defined in ~/.ssh/known_hosts file, otherwise do not verify.")
                    .renderingOption(StringRenderingConstants.GROUP_NAME,"SSH Settings")
                    .build()
            )
            .property(PropertyBuilder.builder()
                    .string("errorKeywords")
                    .title("Error Keywords")
                    .description("If the command output contains any of these keywords, the step will fail. Use commas (,) to separate multiple values.")
                    .renderingOption(StringRenderingConstants.GROUP_NAME, "Failure Matching")
                    .required(false)
                    .build()
            )
            .build();
   }

   /**
     * This enum lists the known reasons this plugin might fail
     */
   enum Reason implements FailureReason{
       SSHKeyNotFound,
       KeyStorage
   }

      @Override
      public void executeNodeStep(final PluginStepContext context,
                                  final Map<String, Object> configuration,
                                  final INodeEntry entry) throws NodeStepException {

          String userName = entry.getAttributes().get("username");
          String hostname = entry.getAttributes().get("hostname");
          String keywordString = (String) configuration.get("errorKeywords");
          String sshKeyStoragePath;
          boolean usePrivKey;
          String sshPrivKey;

          String strictHostKey = (String) configuration.get("strictHostKey");

          if (entry.getAttributes().get("ssh-password-storage-path") != null) {
              sshKeyStoragePath = entry.getAttributes().get("ssh-password-storage-path");
              usePrivKey = false;
          } else if (entry.getAttributes().get("ssh-key-storage-path") != null) {
              sshKeyStoragePath = entry.getAttributes().get("ssh-key-storage-path");
              usePrivKey = true;
          } else {
              throw new NodeStepException("SSH Key or Password must be defined as node attribute.", Reason.SSHKeyNotFound, entry.getNodename());
          }
          sshPrivKey = PluginUtil.getPasswordFromKeyStorage(sshKeyStoragePath, context, entry);

          Gson gson = new GsonBuilder().create();
          JsonArray customFields = gson.fromJson(configuration.get("custom").toString(), JsonArray.class);

          if(customFields != null ) {

              try {

                  SSHConnect connection = new SSHConnect(userName,sshPrivKey,hostname,usePrivKey, strictHostKey);
                  Session session = connection.connect();

                  Channel channel = session.openChannel("shell");
                  OutputStream ops = channel.getOutputStream();
                  PrintStream ps = new PrintStream(ops, true);
                  channel.connect();
                  InputStream input = channel.getInputStream();

                  for (JsonElement customField : customFields) {
                      JsonObject cmdJson = customField.getAsJsonObject();
                      String rawCommand = cmdJson.get("value").getAsString();

                      ps.println(rawCommand);
                      Thread.sleep(500);
                  }

                  ps.close();
                  SSHConnect.printResult(input, channel, keywordString);

                  channel.disconnect();
                  session.disconnect();

              } catch (Exception e) {
                  throw new NodeStepException(
                          "SSH command execution failed: " + e.getMessage(),
                          e,
                          Reason.KeyStorage,
                          entry.getNodename()
                  );
              }
          }
      }
}