This adds workflow steps to integrate with ServiceNow. Includes the following:
- Task:
  - View
  - Add note
  - Set state

These use the [Table API](https://developer.servicenow.com/dev.do#!/reference/api/latest/rest/c_TableAPI) in ServiceNow.

## Installation
Install in the usual way, eg following [Rundeck docs](https://docs.rundeck.com/docs/administration/configuration/plugins/installing.html).
### From package
- Download jar file from https://github.com/gaddman/rundeck-servicenow-plugin/releases/latest
- Copy to Rundeck folder `/var/lib/rundeck/libext/`
### From source
- Clone: `git clone https://github.com/gaddman/rundeck-servicenow-plugin.git`
- Build: `gradle clean build`
- Copy to Rundeck: `cp build/libs/rundeck-servicenow-plugin.jar /var/lib/rundeck/libext/`

## Configuration
ServiceNow authentication properties can be configured system-wide through the GUI at _System Configuration_, or through the `framework.properties`:
```
framework.plugin.PluginGroup.ServiceNowCommunity.url = https://my-company.service-now.com.
framework.plugin.PluginGroup.ServiceNowCommunity.username = my_snow_user
framework.plugin.PluginGroup.ServiceNowCommunity.password_key_storage_path = keys/service-now
```
These properties can be overridden in the job if required.
