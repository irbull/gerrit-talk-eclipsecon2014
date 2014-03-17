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

Gerrit.install(function(self) {
    function onCreateWorkItem(c) {
      var f = c.textfield();
      var b = c.button('Create', {onclick: function(){
        c.call(
          {bugnumber: f.value},
          function(r) {
            c.hide();
            window.location.href=r;
          });
      }});
      c.popup(c.div(
        c.prependLabel('Eclipse Bug Number: ', f),
        c.br(),
        b));
      f.focus();
    }
    self.onAction('project', 'create-item', onCreateWorkItem);
  });
