angular.module("app").controller("showProjectController", ["$scope", "$routeParams", "restService", ($scope, $routeParams, restService) ->
  restService.retrieveProject($routeParams.ownerName, $routeParams.projectName)
    .success((project) -> $scope.project = project)
])
