package gaddman.rundeck.plugins.servicenow

import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.plugins.config.PluginGroup
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder


@Plugin(name = "ServiceNowCommunity", service = ServiceNameConstants.PluginGroup)
public class ServiceNowCommunity implements PluginGroup, Describable {

  public static final String CONFIG_URL = "url"
  public static final String CONFIG_USERNAME = "username"
  public static final String CONFIG_PASSWORD_KEY_STORAGE_PATH = "password_key_storage_path"

  @PluginProperty()
  private String url
  @PluginProperty()
  private String username
  @PluginProperty()
  private String password_key_storage_path

  public static Property propertyUrl = PropertyBuilder.builder()
    .string(CONFIG_URL)
    .title("ServiceNow URL")
    .description("The base URL for ServiceNow, eg https://my-company.service-now.com.")
    .required(false)
    .build()

  public static Property propertyUsername = PropertyBuilder.builder()
    .string(CONFIG_USERNAME)
    .title("User name")
    .description("The ServiceNow username. Must have appropriate permissions.")
    .required(false)
    .build()


  public static Property propertyPassword_key_storage_path = PropertyBuilder.builder()
    .string(CONFIG_PASSWORD_KEY_STORAGE_PATH)
    .title("Password Key Storage Path")
    .description("Key containing the password.")
    .required(false)
    .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY, "STORAGE_PATH")
    .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
    .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
    .build()

  @Override
  public Description getDescription() {
    return DescriptionBuilder.builder()
    .name("ServiceNowCommunity")
    .title("ServiceNow (Community)")
    .description("Common configurations for Community version of ServiceNow plugins.")
    .property(this.propertyUrl)
    .property(this.propertyUsername)
    .property(this.propertyPassword_key_storage_path)
    .build()
  }

  public String get(String propertyName) {
    return this."$propertyName"
  }

  public String getFromKeyStorage(PluginStepContext context, String path){
    ResourceMeta contents = null
    try {
      contents = context.getExecutionContext().getStorageTree().getResource(path).getContents()
    } catch (Exception e) {
      throw new PluginException ("Error accessing key storage at $path", e)
    }
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
    contents.writeContent(byteArrayOutputStream)
    String password = new String(byteArrayOutputStream.toByteArray())
    return password
  }

}
