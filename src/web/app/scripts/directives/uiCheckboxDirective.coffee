angular.module("app").directive("uiCheckbox", ["$compile", ($compile) ->
  restrict: "E"
  replace: true
  require: "ngModel"
  scope:
    label: "@"
  templateUrl: "/scripts/directives/uiCheckboxDirective.html"
  link: ($scope, elements, attrs) ->
    input = elements.find("input")
    input.attr("ng-model", attrs.ngModel)
    input.attr("ng-disabled", attrs.ngDisabled) if attrs.ngDisabled?
    $compile(elements)($scope.$parent)
])
