angular.module("app").controller("showProjectController", ["$scope", "$routeParams", "$data", ($scope, $routeParams, $data) ->
  $scope.ownerName = $routeParams.ownerName
  $scope.projectName = $routeParams.projectName
  $scope.project = $data.project.data
])

angular.module("app").factory("showProjectController$Data", ["restService", (restService) -> ($routeParams) ->
  project: restService.retrieveProject($routeParams.ownerName, $routeParams.projectName)
])
