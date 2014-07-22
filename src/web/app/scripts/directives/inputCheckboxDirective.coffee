angular.module("app").directive("inputCheckbox", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
  template: """<div class="checkbox">
      <label><input type="checkbox" ng-model="value"> {{label}}</label>
    </div>"""
)
