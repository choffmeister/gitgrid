angular.module("app").controller("homeController", ["$scope", "restService", ($scope, restService) ->
  restService.listProjects().success((projects) -> $scope.projects = projects)
])
