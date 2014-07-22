angular.module("app").controller("usersController", ["$scope", "restService", ($scope, restService) ->
  restService.listUsers().success((users) -> $scope.users = users)
])
