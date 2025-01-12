<%--
Copyright (C) 2019 Daniele Baggio @baxtheman
This program is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.
You should have received a copy of the GNU General Public License along with this program. If
not, see <http://www.gnu.org/licenses/>.
--%>

<%@ include file="init.jsp" %>

<%@ page import="com.cara.utils.logviewer.portlets.LogViewerPortlet" %>
<%@page import="com.cara.utils.logviewer.portlets.PortletPropsValues" %>

<liferay-ui:success
	key="success"
	message="ui-request-processed-successfully"
/>
<liferay-ui:error
	key="error"
	message="ui-request-processed-error"
/>
<script type="text/javascript">
	window.errorThreshold = 10;
	window.consecutiveErrorCount = 0;
	window.resourcePointer = "-1";
	var preId = '<portlet:namespace/>viewlog';
	function poll() {
		var resourceMappingUrl = '<portlet:resourceURL/>';
		AUI().use('aui-io-request', function(A) {
			A.io.request(resourceMappingUrl, {
				method: 'POST', data: {
					"<portlet:namespace/><%= LogViewerPortlet.ATTRIB_POINTER %>": window.resourcePointer
				},
				dataType: 'json',
				on: {
					success: function() {
						try {
							if (typeof this.get('responseData') != 'undefined') {
								window.resourcePointer = this.get('responseData').pointer;
								document.getElementById(preId).innerHTML = document.getElementById(preId).innerHTML + this.get('responseData').content;
								document.getElementById("viewlogmode").innerHTML = this.get('responseData').mode;
								window.consecutiveErrorCount=0;
								window.pollingIntervalId = setTimeout(poll, <%= String.valueOf(PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL) %>);
							} else {
								window.consecutiveErrorCount++;
								if (window.consecutiveErrorCount >= window.errorThreshold) {
									clearTimeout(window.pollingIntervalId);
									alert("Polling of the log has been stopped as the poll error limit has been reached. Please refresh the page to restart polling.");
									document.getElementById(preId).innerHTML = document.getElementById(preId).innerHTML + "\n\n\n------\nPolling of the log has been stopped as the poll error limit has been reached. Please refresh the page to restart polling.\n------";
								}else{
									window.pollingIntervalId = setTimeout(poll, <%= String.valueOf(PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL) %>);
								}
							}
						} catch(err) {
							window.consecutiveErrorCount++;
							if (window.consecutiveErrorCount >= window.errorThreshold) {
								clearTimeout(window.pollingIntervalId);
								alert("Polling of the log has been stopped as the poll error limit has been reached. Please refresh the page to restart polling.");
								document.getElementById(preId).innerHTML = document.getElementById(preId).innerHTML + "\n\n\n------\nPolling of the log has been stopped as the poll error limit has been reached. Please refresh the page to restart polling.\n------";
							}else{
								window.pollingIntervalId = setTimeout(poll, <%= String.valueOf(PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL) %>);
							}
						}
					},
					failure: function() {
						window.consecutiveErrorCount++;
						if (window.consecutiveErrorCount >= window.errorThreshold) {
							clearTimeout(window.pollingIntervalId);
							alert("Polling of the log has been stopped as the poll error limit has been reached. Please refresh the page to restart polling.");
							document.getElementById(preId).innerHTML = document.getElementById(preId).innerHTML + "\n\n\n------\nPolling of the log has been stopped as the poll error limit has been reached. Please refresh the page to restart polling.\n------";
						}else{
							window.pollingIntervalId = setTimeout(poll, <%= String.valueOf(PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL) %>);
						}
					}
				}
			});
		});
	}
	function detachlogger() {
		clearTimeout(window.pollingIntervalId);
		return sendCmd('<%= LogViewerPortlet.OP_DETACH %>', $("#<portlet:namespace/>log-selector").val());
	}
	function attachlogger() {
		window.pollingIntervalId = setTimeout(poll, <%= String.valueOf(PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL) %>);
		return sendCmd('<%= LogViewerPortlet.OP_ATTACH %>', $("#<portlet:namespace/>log-selector").val());
	}
	function clearlogger() {
		document.getElementById(preId).innerHTML = '';
	}
	function sendCmd(mycmd, pkgname) {
		var resourceMappingUrl = '<portlet:resourceURL/>';
		AUI().use('aui-io-request', function(A) {
			A.io.request(resourceMappingUrl, {
				method: 'POST', data: {
					"<portlet:namespace/><%= LogViewerPortlet.PARAM_OP %>": mycmd,
					"<portlet:namespace/><%= LogViewerPortlet.PARAM_NAME %>": pkgname
				},
				dataType: 'json',
				on: {
					success: function() {
						var result = this.get('responseData').result;
						if (result == '<%= LogViewerPortlet.RESULT_ERROR %>') {
							alert(this.get('responseData').error);
						}
						var mode = this.get('responseData').mode;
						if(mode !== 'undefined'){
							document.getElementById("viewlogmode").innerHTML = mode;
							if(mode== '<%= LogViewerPortlet.MODE_ATTACHED  %>'){
								$('#attach-btn').prop( "disabled", true );
								$("#<portlet:namespace/>log-selector").prop( "disabled", true );
							}else if(mode== '<%= LogViewerPortlet.MODE_DETACHED  %>'){
								$('#attach-btn').prop( "disabled", false );
								$("#<portlet:namespace/>log-selector").prop( "disabled", false );
							}
						}
					}
				}
			});
		});
	}
// 	console.log("setInterval");
<%-- 	window.pollingIntervalId = setInterval(poll, <%= String.valueOf(PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL) %>); --%>
</script>
<div class="container">
	<aui:select name="log-selector" label="Log selector">
		<aui:option value="">Liferay</aui:option>
		<aui:option value="com.cara">Cara</aui:option>
		<aui:option value="com.cara.service.helper">Cara service helper</aui:option>
	</aui:select>
	<div class="alert alert-info" role="alert">
		<span class="alert-indicator">
			<svg class="lexicon-icon lexicon-icon-info-circle" focusable="false" role="presentation">
				<use href="/images/icons/icons.svg#info-circle"></use>
			</svg>
		</span>
		<liferay-ui:message key="the-logger-is-currently" />
		<strong><span id="viewlogmode"><liferay-ui:message key="waiting-for-status" /></span>.</strong>
		<liferay-ui:message arguments="<%= new String[] {PortletPropsValues.PERMEANCE_LOG_VIEWER_REFRESH_INTERVAL_DISPLAY_SECONDS} %>" key="polling-every-x-seconds" />
	</div>
	<div class="navbar navbar-collapse-absolute navbar-expand-md ">
	<input id="attach-btn" class="btn btn-primary btn-sm" onClick="attachlogger(); return false;" type="button" value="<liferay-ui:message key="attach-logger" />" />
	<input id="clear-btn" class="btn btn-secondary btn-sm" onClick="clearlogger(); return false;" type="button" value="<liferay-ui:message key="clear-logger" />" />
	<input id="detach-btn" class="btn btn-secondary btn-sm" onClick="detachlogger(); return false;" type="button" value="<liferay-ui:message key="detach-logger" />" />
	</div>
	<pre id="<portlet:namespace/>viewlog">
	</pre>
</div>
<style>
#<portlet:namespace/>viewlog {
	margin: 10px 0px;
	min-height: 200px;
	background-color: #333;
	color: #ccc;
	font-size: 11px;
	max-height: 60vh;
	overflow: auto;
}
</style>