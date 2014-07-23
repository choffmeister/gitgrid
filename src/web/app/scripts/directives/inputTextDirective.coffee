angular.module("app").directive("inputText", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
    placeholder: "@"
  template: """<div class="form-group">
      <label class="control-label">{{label}}</label>
      <input class="form-control" type="text" name="name" placeholder="{{placeholder}}" ng-model="value" />
    </div>"""
)
