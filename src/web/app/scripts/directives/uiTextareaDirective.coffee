angular.module("app").directive("uiTextarea", () ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    disabled: "="
    label: "@"
    placeholder: "@"
    rows: "@"
  templateUrl: "/scripts/directives/uiTextareaDirective.html"
)
