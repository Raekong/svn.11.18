define(
    [],
    function(){
        function SubmitChecker( ec ){
            this.name = "checker";
            this.ec = ec;
            this.ec.addHandler(this);

            var _that = this;

            this.repeatArrCheck = function(arr1, arr2, key){
                for(var i=0; i<arr1.length; i++){
                    for(var j=0; j<arr2.length; j++){
                        if( i == j ) continue;
                        if( arr1[i][key] == arr2[j][key] ){
                            return true;
                        }
                    }
                }
                return false;
            }

            this.repeatCheck = function(arr, key){
                for(var i=0; i<arr.length; i++){
                    for(var j=0; j<arr.length; j++){
                        if( i == j ) continue;
                        if( arr[i][key] == arr[j][key] ){
                            return true;
                        }
                    }
                }
                return false;
            }


            this.check = function(data){
                console.log(data);
                var info = null;
                if(!data.acceptFlag){
                    info = '请确认投稿事项!'
                };
                if(!data.sectionId) {
                    info = '请选择投稿栏目!'
                }

                if(!data.uploadFiles || data.uploadFiles.length == 0 ) {
                    //文件名类型名检查
                    info = '请上传文章材料!';
                }else{
                    var flag = _that.repeatCheck(data.uploadFiles, 'originName');
                    if( flag ){
                        info = '上传材料文件名有重复，请检查';
                    }
                }

                if(!data.authors || data.authors.length == 0 ) {
                   //文件名类型名检查
                   info = '请添加文章作者信息!';
                }else{
                    var flag = _that.repeatCheck(data.authors, 'email');
                    if( flag ){
                        info = '添加的文章作者姓名或邮箱有重复，请检查';
                    }
                }

                
                if(!data.title){
                    info = '请填写文章标题!'
                }

                if(!data.keywords){
                    info = '请填写文章关键词!'
                }

                if(!data.abstract){
                    info = '请填写文章摘要!'
                }else{
                    data.abstractTxt = data.abstract;
                }

                if($('#addReviewer')){
                    if(!data.reviewers || data.reviewers.length == 0 ) {
                        info = '请添加推荐审稿人信息!';
                    }else{
                        var flag = _that.repeatCheck(data.reviewers, 'email');
                        if( flag ){
                            info = '添加的推荐审稿人有重复，请检查';
                        }
                    }
                }

                if( _that.repeatArrCheck(data.authors, data.reviewers, 'email')){
                    info = '文章作者不能同时作为审稿人，请检查';
                };

                if(!!info){
                    _that.ec.fire(
                        'checker',
                        'checkerror',
                        info
                    )
                }else{
                    $.post(
                        '/submit/do',
                        { obj: JSON.stringify(data) },
                        function( rst ){
                            if( !!rst ){
                                _that.ec.fire(
                                    'checker',
                                    'checkerror',
                                    rst
                                )
                                return;
                            }
                            _that.ec.fire(
                                'checker',
                                'submitdone',
                                null
                            )
                        }
                    )
                }
            }

            this.exports = {
                "submitCheck" : this.check
            }
        }
        return SubmitChecker;
    }
)