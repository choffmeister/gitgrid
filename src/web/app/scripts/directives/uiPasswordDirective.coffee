angular.module("app").directive("uiPassword", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    disabled: "="
    label: "@"
    placeholder: "@"
  templateUrl: "/scripts/directives/uiPasswordDirective.html"
)
