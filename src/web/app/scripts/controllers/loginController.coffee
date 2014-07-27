angular.module("app").controller("loginController", ["$scope", "$location", "authService", ($scope, $location, authService) ->
  $scope.userName = "user1"
  $scope.password = "pass1"
  $scope.message = null

  $scope.login = () ->
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
      )
      .error((err) ->
        $scope.password = ""
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
