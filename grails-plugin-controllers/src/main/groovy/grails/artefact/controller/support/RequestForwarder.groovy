/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.artefact.controller.support

import grails.web.UrlConverter
import grails.web.api.WebAttributes
import grails.web.mapping.LinkGenerator
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic
import org.grails.plugins.web.controllers.metaclass.ForwardMethod
import org.grails.web.mapping.ForwardUrlMappingInfo
import org.grails.web.mapping.UrlMappingUtils
import org.grails.web.mapping.mvc.UrlMappingsHandlerMapping
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.util.WebUtils
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.DataBinder
import org.springframework.web.context.request.WebRequest
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.RequestDispatcher

/**
 * A Trait for classes that forward the request
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait RequestForwarder implements WebAttributes {
    private UrlConverter urlConverter
    private LinkGenerator linkGenerator

    @Autowired(required=false)
    void setUrlConverter(UrlConverter urlConverter) {
        this.urlConverter = urlConverter
    }

    private LinkGenerator lookupLinkGenerator() {
        if(this.linkGenerator == null) {
            this.linkGenerator = webRequest.getApplicationContext().getBean(LinkGenerator)
        }
        return this.linkGenerator
    }

    /**
     * Forwards a request for the given parameters using the RequestDispatchers forward method
     *
     * @param params The parameters
     * @return The forwarded URL
     */
    String forward(Map params) {

        GrailsWebRequest webRequest = getWebRequest()

        if (webRequest) {
            def controllerName
            if(params.controller) {
                controllerName = params.controller
            } else {
                controllerName = webRequest.controllerName
            }

            if(controllerName) {
                def convertedControllerName = convert(controllerName.toString())
                webRequest.controllerName = convertedControllerName
            }
            params.controller = webRequest.controllerName

            if(params.action) {
                params.action = convert(params.action.toString())
            }

            if(params.namespace) {
                params.namespace = params.namespace
            }

            if(params.plugin) {
                params.plugin = params.plugin
            }
        }

        def model = params.model instanceof Map ? params.model : Collections.EMPTY_MAP

        def request = webRequest.currentRequest
        def response = webRequest.currentResponse

        WebUtils.exposeRequestAttributes(request, (Map)model);

        request.setAttribute(GrailsApplicationAttributes.FORWARD_IN_PROGRESS, true)
        request.setAttribute(GrailsApplicationAttributes.FORWARD_ISSUED, true)

        def fowardURI = lookupLinkGenerator().link(params)


        RequestDispatcher dispatcher = request.getRequestDispatcher(fowardURI)
        webRequest.removeAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0);
        webRequest.removeAttribute(GrailsApplicationAttributes.GRAILS_CONTROLLER_CLASS_AVAILABLE, WebRequest.SCOPE_REQUEST);
        webRequest.removeAttribute(UrlMappingsHandlerMapping.MATCHED_REQUEST, WebRequest.SCOPE_REQUEST);
        webRequest.removeAttribute("grailsWebRequestFilter" + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, WebRequest.SCOPE_REQUEST);
        dispatcher.forward(request, response);
        return fowardURI
    }


    private String convert(String value) {
        (urlConverter) ? urlConverter.toUrlElement(value) : value
    }
}