angular.module("app").controller("homeController", ["$scope", "$data", ($scope, $data) ->
  $scope.projects = $data.projects.data
])

angular.module("app").factory("homeController$Data", ["restService", (restService) -> ($routeParams) ->
  projects: restService.listProjects()
])
