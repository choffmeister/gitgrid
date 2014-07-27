angular.module("app").controller("loginController", ["$scope", "$location", "authService", "flashService", ($scope, $location, authService, flashService) ->
  $scope.userName = ""
  $scope.password = ""
  $scope.busy = false

  $scope.login = () -> if not $scope.busy
    $scope.busy = true
    authService.login($scope.userName, $scope.password)
      .success((res) ->
        $scope.password = ""
        if res.user?
          $location.path("/")
        else
          flashService.warning("The credendials are invalid.")
        $scope.busy = false
      )
      .error((err) ->
        $scope.password = ""
        $scope.busy = false
      )
])
