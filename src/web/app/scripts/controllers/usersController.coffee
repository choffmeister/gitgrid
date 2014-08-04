angular.module("app").controller("usersController", ["$scope", "$data", ($scope, $data) ->
  $scope.users = $data.users.data
])

angular.module("app").factory("usersController$Data", ["restService", (restService) -> ($routeParams) ->
  users: restService.listUsers()
])
