/*
 *  Copyright 2023 predic8 GmbH, www.predic8.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.predic8.membrane.core.interceptor.javascript;

import com.predic8.membrane.core.*;
import com.predic8.membrane.core.lang.javascript.*;

public class RhinoJavascriptLanguageAdapter extends LanguageAdapter {

    public RhinoJavascriptLanguageAdapter(Router router) {
        super(router);
        languageSupport = new RhinoJavascriptLanguageSupport();
    }

    /**
     * new java.lang.String(s) is needed to avoid ambiguity.
     */
    protected String prepareScript(String script) {
        return """
            var imports = new JavaImporter(com.predic8.membrane.core.interceptor.Outcome, com.predic8.membrane.core.http)
            var console = {};
            console.log = function(s) {
                java.lang.System.out.println(new java.lang.String(s));
            };
            console.log("Body" +  message.getBodyAsStringDecoded() +"T");
            console.log("Empty" +  message.isBodyEmpty());
            var json;
            if (!message.isBodyEmpty()) json=JSON.parse(message.getBodyAsStringDecoded());
            with(imports) {
            """
               + script +
            """ 
            
            };
            """;
    }
}
