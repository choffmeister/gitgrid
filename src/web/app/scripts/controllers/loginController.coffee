angular.module("app").controller("loginController", ["$scope", "$location", "authService", "flashService", ($scope, $location, authService, flashService) ->
  $scope.userName = ""
  $scope.password = ""
  $scope.busy = false

  $scope.login = () -> if not $scope.busy
    $scope.busy = true
    authService.login($scope.userName, $scope.password)
      .success((res) ->
        if not res.user?
          $scope.password = ""
          $scope.busy = false
          $scope.focus()
          flashService.warning("The credendials are invalid.")
        else
          $location.path("/")
      )
      .error((err) ->
        $scope.password = ""
        $scope.busy = false
        $scope.focus()
      )

  $scope.focus = () ->
    if $scope.userName == "" or not $scope.userName
      $scope.$broadcast("focusUserName")
    else
      $scope.$broadcast("focusPassword")
])
