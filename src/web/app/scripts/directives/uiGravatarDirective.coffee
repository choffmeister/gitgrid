angular.module("app").directive("uiGravatar", () ->
  restrict: "E"
  replace: true
  scope:
    email: "="
  link: ($scope, element, attrs) ->
    currentSize = 0
    reloadTreshold = 1.5
    updateImageUrl = () ->
      if $scope.email?
        newSize = Math.max(element.width(), element.height())
        if newSize > currentSize * reloadTreshold
          currentSize = newSize
          protocol = window.location.protocol
          density = window.devicePixelRatio or 1
          md5 = CryptoJS.MD5($scope.email)
          $scope.imageUrl = "#{protocol}//www.gravatar.com/avatar/#{md5}.jpg?s=#{currentSize * density}&d=mm"
    $(window).on "resize", () -> updateImageUrl()
    $scope.$watch "email", () -> updateImageUrl()
  templateUrl: "/scripts/directives/uiGravatarDirective.html"
)
