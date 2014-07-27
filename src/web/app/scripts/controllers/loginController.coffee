angular.module("app").controller("loginController", ["$scope", "$location", "authService", ($scope, $location, authService) ->
  $scope.userName = ""
  $scope.password = ""
  $scope.message = null
  $scope.busy = false

  $scope.login = () -> if not $scope.busy
    $scope.busy = true
    authService.login($scope.userName, $scope.password)
      .success((res) ->
        $scope.password = ""
        if res.user?
          $scope.message = null
          $location.path("/")
        else
          $scope.message =
            type: "warn"
            text: res.message
        $scope.busy = false
      )
      .error((err) ->
        $scope.password = ""
        $scope.message =
          type: "error"
          text: "An unknown error occured"
        $scope.busy = false
      )
])
