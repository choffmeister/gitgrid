angular.module("app").directive("inputPassword", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
    placeholder: "@"
  template: """<div class="form-group">
      <label class="control-label">{{label}}</label>
      <input class="form-control" type="password" name="name" placeholder="{{placeholder}}" ng-model="value" />
    </div>"""
)
