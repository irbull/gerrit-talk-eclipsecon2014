// Copyright (C) 2013 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.tutorialplugin.client;

import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.Set;

class IndexScreen extends VerticalPanel {
  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Gerrit Issue Tracking");
      screen.show(new IndexScreen());
    }
  }

  private TextBox summaryTxt;
  private TextArea descriptionTxt;
  private ListBox listBox;

  IndexScreen() {
    setStyleName("cookbook-panel");
    Panel project = new VerticalPanel();
    project.add(new Label("Project: " ));
    listBox = new ListBox();
    project.add(listBox);
    add(project);

    new RestApi("projects").view("").get(new AsyncCallback<JavaScriptObject>() {

      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(JavaScriptObject result) {
        JSONObject jsonObject = new JSONObject(result);
        Set<String> keySet = jsonObject.keySet();
        for (String string : keySet) {
          listBox.addItem(string);
        }
      }});

    Panel summaryLabel = new VerticalPanel();
    summaryLabel.add(new Label("Summary:"));
    summaryTxt = new TextBox() {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if (event.getTypeInt() == Event.ONPASTE) {
          Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
              if (getValue().trim().length() != 0) {
                setEnabled(true);
              }
            }
          });
        }
      }
    };
    summaryTxt.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        event.stopPropagation();
      }
    });
    summaryTxt.sinkEvents(Event.ONPASTE);
    summaryTxt.setVisibleLength(72);
    summaryLabel.add(summaryTxt);
    add(summaryLabel);

    Panel descriptionLabel = new VerticalPanel();
    descriptionLabel.add(new Label("Description:"));
    descriptionTxt = new TextArea();
    descriptionTxt.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        event.stopPropagation();
      }
    });
    descriptionTxt.setVisibleLines(12);
    descriptionTxt.setCharacterWidth(80);
    descriptionTxt.getElement().setPropertyBoolean("spellcheck", false);
    descriptionLabel.add(descriptionTxt);
    add(descriptionLabel);

    Button helloButton = new Button("Submit");
    helloButton.addStyleName("cookbook-helloButton");
    helloButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        sayHello();
      }
    });
    add(helloButton);
    helloButton.setEnabled(true);
  }

  private void sayHello() {


    JSONObject jsonObject = new JSONObject();
    jsonObject.put("summary", new JSONString(summaryTxt.getText()));
    jsonObject.put("description", new JSONString(descriptionTxt.getText()));
    new RestApi("projects").id(listBox.getItemText(listBox.getSelectedIndex())).view("tutorialPlugin", "create-item").post(jsonObject.getJavaScriptObject(), new AsyncCallback<JavaScriptObject>() {

      @Override
      public void onSuccess(JavaScriptObject result) {
        Window.Location.assign(result.toString());
      }

      @Override
      public void onFailure(Throwable caught) {
        // never invoked
        Window.alert("Fail");
      }
    });
  }
}
