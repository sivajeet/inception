/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.FeatureState;

public class DynamicTextAreaFeatureEditor
    extends TextFeatureEditorBase
{
    private static final long serialVersionUID = -4798925191252992223L;
    private TextArea<String> textarea;

    public DynamicTextAreaFeatureEditor(String aId, MarkupContainer aItem,
            IModel<FeatureState> aModel)
    {
        super(aId, aItem, aModel);
    }

    @Override
    protected AbstractTextComponent createInputField()
    {
        textarea = new TextArea<>("value");
        textarea.setOutputMarkupId(true);
        return textarea;
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        aResponse.render(JavaScriptHeaderItem.forReference(DynamicTextAreaScriptReference.get()));
        aResponse.render(OnDomReadyHeaderItem.forScript(
                "window.addEventListener('load', function(){resizeDynamicTextArea(document.getElementById('"
                        + textarea.getMarkupId() + "'));});"));
    }
}
