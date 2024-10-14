package gaddman.rundeck.plugins.servicenow

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.plugins.PluginLogger
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils

public class ServiceNowClient {
  private final String baseUrl
  private final HttpClient httpClient
  private final PluginLogger logger

  public ServiceNowClient(PluginLogger logger, String url, String username, String password) {
    this.logger = logger
    // Service Now endpoint.
    this.baseUrl = (url.endsWith("/") ? url : url + "/") + "api/now"
    logger.log(4, "Connecting to ServiceNow with: url=${this.baseUrl}, username=${username}")
    // Authentication.
    CredentialsProvider credsProvider = new BasicCredentialsProvider()
    credsProvider.setCredentials(
      AuthScope.ANY,
      new UsernamePasswordCredentials(username, password)
    )
    // Proxy.
    String proxyHost = System.getProperty("https.proxyHost") ?: System.getProperty("http.proxyHost")
    String proxyPort = System.getProperty("https.proxyPort") ?: System.getProperty("http.proxyPort")
    HttpHost proxy = proxyHost && proxyPort ? new HttpHost(proxyHost, Integer.parseInt(proxyPort)) : null
    // Headers.
    List<BasicHeader> defaultHeaders = [
      new BasicHeader("Content-Type", "application/json"),
      new BasicHeader("Accept", "application/json")
    ]
    // Create client.
    this.httpClient = HttpClients.custom()
      .setDefaultCredentialsProvider(credsProvider)
      .setDefaultHeaders(defaultHeaders)
      .setProxy(proxy)
      .build();
  }

  private Map<String, Object> ApiGet(String url) throws StepException {
    this.logger.log(4, "GET to ${url}")
    HttpGet request = new HttpGet(url)
    HttpResponse response = httpClient.execute(request)
    int statusCode = response.getStatusLine().getStatusCode()

    if (statusCode == 200) {
      String responseBody = EntityUtils.toString(response.getEntity());
      this.logger.log(4, "Response: ${responseBody}")
      Map jsonResponse = new JsonSlurper().parseText(responseBody)
      return jsonResponse.get("result")[0]
    } else {
      this.logger.log(0, "Failed request. Status code: ${statusCode}")
      throw new StepException("ServiceNow API request failed with status code: ${statusCode}", StepFailureReason.IOFailure)
    }
  }

  private Map<String, Object> ApiPatch(String url, Map data) throws StepException {
    this.logger.log(4, "PATCH to ${url}")
    HttpPatch request = new HttpPatch(url)
    request.setEntity(new StringEntity(JsonOutput.toJson(data)))
    HttpResponse response = httpClient.execute(request);
    int statusCode = response.getStatusLine().getStatusCode();

    if (statusCode == 200 || statusCode == 201) {
      String responseBody = EntityUtils.toString(response.getEntity());
      this.logger.log(4, "Response: ${responseBody}")
      Map jsonResponse = new JsonSlurper().parseText(responseBody)
      return jsonResponse.get("result")
    } else {
      this.logger.log(0, "Failed request. Status code: ${statusCode}")
      throw new StepException("ServiceNow API request failed with status code: ${statusCode}", StepFailureReason.IOFailure)
    }
  }

  private Map<String, Object> ApiPost(String url, Map data) throws StepException {
    this.logger.log(4, "POST to ${url}")
    this.logger.log(4, "Data: ${data}")
    HttpPost request = new HttpPost(url)
    request.setEntity(new StringEntity(JsonOutput.toJson(data)))
    HttpResponse response = httpClient.execute(request);
    int statusCode = response.getStatusLine().getStatusCode();

    if (statusCode == 200 || statusCode == 201) {
      String responseBody = EntityUtils.toString(response.getEntity());
      this.logger.log(4, "Response: ${responseBody}")
      Map jsonResponse = new JsonSlurper().parseText(responseBody)
      return jsonResponse.get("result")
    } else {
      this.logger.log(0, "Failed request. Status code: ${statusCode}")
      throw new StepException("ServiceNow API request failed with status code: ${statusCode}", StepFailureReason.IOFailure)
    }
  }

  public Map<String, Object> taskGetDetails(String tasknum) {
    String url = baseUrl + "/table/sc_task?sysparm_query=number=" + tasknum + "&sysparm_limit=1";
    return this.ApiGet(url)
  }

  public void taskAddNote(String tasknum, String note) {
    Map taskDetails = this.taskGetDetails(tasknum)
    String sysId = taskDetails.get("sys_id")
    String url = baseUrl + "/table/sc_task/" + sysId + "?sysparm_limit=1"
    def noteData = [work_notes: note]
    this.ApiPatch(url, noteData)
  }

  public void taskSetState(String tasknum, String state) {
    Map<String, Integer> stateValues = [
      "Not Started"       : -10,
      "Pending"           : -5,
      "Open"              :  1,
      "Work in Progress"  :  2,
      "Closed Complete"   :  3,
      "Closed Incomplete" :  4,
      "Closed Skipped"    :  7,
      "Closed Cancelled"  :  8,
    ]
    Map taskDetails = this.taskGetDetails(tasknum)
    String sysId = taskDetails.get("sys_id")
    String url = baseUrl + "/table/sc_task/" + sysId + "?sysparm_limit=1";
    def stateData = [state: stateValues[state]]
    this.ApiPatch(url, stateData)
  }

}
