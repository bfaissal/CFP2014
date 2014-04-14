/**
 * Created with IntelliJ IDEA.
 * User: faissalboutaounte
 * Date: 2014-03-30
 * Time: 11:17
 * To change this template use File | Settings | File Templates.
 */
//var jmaghreb = angular.module('jmaghreb', []);
jmaghreb.directive('uploader', function ($http) {
    return {
        restrict: 'E',
        replace: true,
        transclude: false,
        scope: {id: '=',
            image: '=',
            url: "="},
        link: function (scope, element, attrs) {
            //attrs.id = attr.id
            try{
                scope.$apply(function () {
                    scope.image = attrs.image;
                })
            }catch(e){
                  alert(e)
            }

            scope.url = attrs.url;
            scope.id = attrs.id;
            scope.isEdit = true;
            scope.inProgress = false;
            $("#" + attrs.id).fileupload({
                acceptFileTypes: '/(\.|\/)(gif|jpe?g|png)$/i',
                maxFileSize: 1000000, // 1 MB
                dataType: 'json',
                url: attrs.url,

                done: function (e, data) {
                    $.each(data.result.files, function (index, file) {
                        try{

                            scope.$apply(function () {
                                //scope.inProgress = false;
                                scope.image = file.name;
                            })
                        }catch(e){
                            alert(e)
                        }
                        $('#progress .bar').css('width','0%');
                    });
                }
            }).on('fileuploadadd', function (e, data) {
                    scope.isEdit = false;
                    scope.inProgress = true;
                    $('#progress .bar').css('width','0%');
                    $.each(data.files, function (index, file) {

                    });
                }).on('fileuploadfail', function (e, data) {
                    alert( " "+data.jqXHR.responseText);
                }).on('fileuploadprogressall', function (e, data) {
                    var progress = parseInt(data.loaded / data.total * 100, 10);
                    scope.inProgress = true;
                    $('#progress .bar').css('width',progress + '%');
                });
            scope.canceUpload = function () {
                $http({method: 'GET', url: '/deleteImage/'+scope.image+'/'+scope.isEdit}).
                    success(function(data, status, headers, config) {
                        $('#progress .bar').css('width','0%');
                    }).
                    error(function(data, status, headers, config) {});
                scope.image = "";

            }
        },
        template: '<div>'+
            '<span id="{{ id }}Btn" ng-show="image == null || image ==\'\'" class="ui labeled icon small button fileinput-button">' +
            '<i class="cloud upload icon"></i>    ' +
            '<span>Select file ...</span>' +
            '<!-- The file input field used as target for the file upload widget -->   ' +
            '<input id="{{id}}" type="file" class="input-xxlarge" name="file"/> ' +
            '</span>       ' +
            '<div id="progress" class="progress input-xxlarge" style="margin-top: 10px;" ng-show="(image == null || image ==\'\')">' +
            '<div class="progress-bar" style="width: 0%"></div>' +
            '</div>'+
            '<div id="progress" class="ui active striped progress" style="margin-top: 10px;" ng-show="(image == null || image ==\'\')">'+
            '<div class="bar" style="width: 0%;"></div>'+
            '</div>' +

            '<div id="{{id}}Holder" ng-show="image != null && image !=\'\'" style="display: block"> ' +
            '<div>' +
            '<img style="width: 150px;" ng-src=\'{{ image }}\'/></div>' +
            '<button class="ui red small button" ng-click="canceUpload()">Delete</button>  ' +
            '</div>' +
            '</div>'

    }
})