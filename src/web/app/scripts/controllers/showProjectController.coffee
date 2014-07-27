angular.module("app").controller("showProjectController", ["$scope", "$routeParams", "restService", ($scope, $routeParams, restService) ->
  $scope.ownerName = $routeParams.ownerName
  $scope.projectName = $routeParams.projectName

  restService.retrieveProject($routeParams.ownerName, $routeParams.projectName)
    .success((project) -> $scope.project = project)
])
