angular.module("app").directive("navigationMenu", () ->
  restrict: "E"
  replace: true
  transclude: true
  scope:
    id: "@"
    brand: "@"
  template: """<nav class="navbar navbar-default" role="navigation">
      <div class="container-fluid">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="/">{{brand}}</a>
        </div>
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1" ng-transclude></div>
      </div>
    </nav>"""
)

angular.module("app").directive("navigationSubMenu", () ->
  restrict: "E"
  replace: true
  transclude: true
  scope:
    label: "="
  template: """<li class="dropdown">
      <a class="dropdown-toggle" href="#" data-toggle="dropdown">
        {{label}} <span class="caret"></span>
      </a>
      <ul class="dropdown-menu" role="menu" ng-transclude></ul>
    </li>"""
)

angular.module("app").directive("navigationEntry", () ->
  restrict: "E"
  replace: true
  scope:
    label: "="
    url: "="
  template: """<li><a href="{{url}}">{{label}}</a></li>"""
)

angular.module("app").directive("navigationDivider", () ->
  restrict: "E"
  replace: true
  template: """<li class="divider"></li>"""
)
