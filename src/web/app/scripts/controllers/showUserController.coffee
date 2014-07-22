angular.module("app").controller("showUserController", ["$scope", "$routeParams", "restService", ($scope, $routeParams, restService) ->
  restService.retrieveUser($routeParams.userName)
    .success((user) -> $scope.user = user)
])
