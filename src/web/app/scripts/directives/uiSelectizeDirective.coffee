angular.module("app").directive("uiSelectize", ["$timeout", ($timeout) ->
  restrict: "E"
  replace: true
  scope:
    value: "="
    label: "@"
    placeholder: "@"
  link: ($scope, element, attrs) ->
    options =
      delimiter: ","
      persist: false
      create: (input) ->
        value: input
        text: input

    selectize = element.find("input").selectize(options)[0].selectize

    selectize.on "change", (newValue) ->
      console.log("selectize.on", $scope.value, newValue)
      if not _.isEqual($scope.value, newValue)
        $timeout () ->
          console.log("set angular value")
          $scope.value = newValue.split(",")

    $scope.$watch "value", (newValue) ->
      console.log("$scope.$watch", selectize.getValue().split(","), newValue)
      if not _.isEqual(selectize.getValue().split(","), newValue)
        console.log("set selectize value")
        _.each(newValue, (x) -> selectize.addOption({ value: x, text: x }))
        selectize.setValue(newValue)
  templateUrl: "/scripts/directives/uiSelectiveDirective.html"
])
