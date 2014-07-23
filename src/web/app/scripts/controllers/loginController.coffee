angular.module("app").controller("loginController", ["$scope", "$location", "authService", ($scope, $location, authService) ->
  $scope.userName = "user1"
  $scope.password = "pass1"
  $scope.message = null

  $scope.login = () ->
    userName = $scope.userName
    password = $scope.password
    $scope.password = ""

    authService.login(userName, password)
      .success((res) ->
        if res.user?
          $scope.message = null
          $location.path("/")
        else
          $scope.message =
            type: "warn"
            text: res.message
      )
      .error((err) ->
        $scope.message =
          type: "error"
          text: "An unknown error occured"
      )
])
