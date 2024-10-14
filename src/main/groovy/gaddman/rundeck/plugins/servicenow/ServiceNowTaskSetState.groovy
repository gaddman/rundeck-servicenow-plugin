package gaddman.rundeck.plugins.servicenow

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.config.ConfiguredBy
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

import gaddman.rundeck.plugins.servicenow.ServiceNowCommunity


@Plugin(name = PLUGIN_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = PLUGIN_TITLE, description = PLUGIN_DESCRIPTION)
public class ServiceNowTaskSetState implements StepPlugin, Describable, ConfiguredBy<ServiceNowCommunity> {

  public static final String PLUGIN_NAME = "servicenow-task-set-state"
  public static final String PLUGIN_TITLE = "ServiceNow / Task / Set state"
  public static final String PLUGIN_DESCRIPTION = "Set the state of a ServiceNow Task"

  private static final String CONFIG_TASKNUM = "taskNum"
  private static final String CONFIG_STATE = "state"

  private ServiceNowCommunity serviceNowPluginGroup

  @Override
  public void setPluginGroup(ServiceNowCommunity pluginGroup){
    this.serviceNowPluginGroup = pluginGroup
  }

  static Description DESCRIPTION = DescriptionBuilder.builder()
    .name(PLUGIN_NAME)
    .title(PLUGIN_TITLE)
    .description(PLUGIN_DESCRIPTION)
    .pluginGroup(ServiceNowCommunity.class)

    // Properties are in the format as described at:
    // https://www.javadoc.io/static/org.rundeck/rundeck-core/5.6.0-20240912/com/dtolabs/rundeck/core/plugins/configuration/PropertyUtil.html#string(java.lang.String,java.lang.String,java.lang.String,boolean,java.lang.String)
    //   Variable name
    //   Title
    //   Description
    //   Required
    //   Default value

    .property(PropertyUtil.string(
      CONFIG_TASKNUM,
      "Task Number",
      "The ServiceNow task number.",
      true,
      null
    ))

    .property(PropertyUtil.select(
      CONFIG_STATE,
      "State",
      "The new state for the task.",
      true,
      null,
      ["Not Started", "Pending", "Open", "Work in Progress", "Closed Complete", "Closed Incomplete", "Closed Skipped", "Closed Cancelled"]
    ))

    // Allow user to override the PluginGroup properties.
    .property(PropertyBuilder.builder(ServiceNowCommunity.propertyUrl)
      .description(ServiceNowCommunity.propertyUrl.getDescription() + " Leave blank if set at project/framework level.")
      .renderingOption(StringRenderingConstants.GROUP_NAME, "Authentication")
      .renderingOption(StringRenderingConstants.GROUPING, "Secondary")
      .build()
    )
    .property(PropertyBuilder.builder(ServiceNowCommunity.propertyUsername)
      .description(ServiceNowCommunity.propertyUsername.getDescription() + " Leave blank if set at project/framework level.")
      .renderingOption(StringRenderingConstants.GROUP_NAME, "Authentication")
      .renderingOption(StringRenderingConstants.GROUPING, "Secondary")
      .build()
    )
    .property(PropertyBuilder.builder(ServiceNowCommunity.propertyPassword_key_storage_path)
      .description(ServiceNowCommunity.propertyPassword_key_storage_path.getDescription() + " Leave blank if set at project/framework level.")
      .renderingOption(StringRenderingConstants.GROUP_NAME, "Authentication")
      .renderingOption(StringRenderingConstants.GROUPING, "Secondary")
      .build()
    )

    .build()

  @Override
  public Description getDescription() {
    return DESCRIPTION
  }

  @Override
  public void executeStep(final PluginStepContext context, final Map<String, Object> configuration) throws StepException {

    String taskNum = configuration.get(CONFIG_TASKNUM);
    String state = configuration.get(CONFIG_STATE);
    PluginLogger logger = context.getLogger()
    // Log levels: [0: Error, 1: Warning, 2: Notice, 3: Info, 4: Debug]
    logger.log(4, "Job config: ${configuration}...")

    // Get authentication config from job if set, otherwise pluginGroup.
    String url = configuration.get(ServiceNowCommunity.CONFIG_URL) ?: this.serviceNowPluginGroup.get(ServiceNowCommunity.CONFIG_URL)
    String username = configuration.get(ServiceNowCommunity.CONFIG_USERNAME) ?: this.serviceNowPluginGroup.get(ServiceNowCommunity.CONFIG_USERNAME)
    String password_key_storage_path = configuration.get(ServiceNowCommunity.CONFIG_PASSWORD_KEY_STORAGE_PATH) ?: this.serviceNowPluginGroup.get(ServiceNowCommunity.CONFIG_PASSWORD_KEY_STORAGE_PATH)

    logger.log(4, "Accessing ServiceNow for task ${taskNum}...")
    String password = serviceNowPluginGroup.getFromKeyStorage(context, password_key_storage_path)

    try {
      ServiceNowClient client = new ServiceNowClient(logger, url, username, password)
      logger.log(3, "Setting state to ${state} for task ${taskNum}...")
      client.taskSetState(taskNum, state)
      logger.log(2, "State set successfully")
    } catch (Exception e) {
      logger.log(0, "Error executing ServiceNow step: " + e.getMessage())
      throw StepException ("Error executing ServiceNow step: " + e.getMessage(), StepFailureReason.Unknown)
    }

  }

}
