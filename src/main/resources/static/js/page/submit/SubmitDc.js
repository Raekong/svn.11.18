define(
    ["datacenter", "widget", "template" , "store"]
    ,function(dc, widget, tpl, store){

        function SumbitDc( ec ){
            this.name = "submitdc";
            this.ec = ec;
            this.ec.addHandler( this );

            this.widget = new widget();
             var _that = this;

            this.getPaper = function(){
                var file = null;
                for(var i=0; i<_that.uploadFiles.length;i++){
                    if( _that.uploadFiles[i].filetype.toLowerCase().indexOf('word')!= -1){
                        return _that.uploadFiles[i];
                    }else if(_that.uploadFiles[i].filetype.toLowerCase().indexOf('pdf')!= -1){
                        file = _that.uploadFiles[i];
                    }
                }
                return file;
            }

            this.acceptFlag =  false;
            this.accept = function( flag ){
                _that.acceptFlag = flag;
            }

            this.sectionId = null;
            this.sectionText = null;
            this.sectionSelect = function(data){
                _that.sectionId = data.id;
                _that.sectionText = data.text;
            }

            var data = store.get('infos');
            console.log(data);

            //=================add reviewer ==================================
            this.reviewers = [];
            this.tmpReviewers = [];
            var reviewersDS = {
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                },
                'data-name': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-affiliation': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-researchfield': {
                    'verify': 'nonull',
                    'val' : ''
                }
            }
            this.newReviwer = function(){
                _that.tmpReviewers = [];
            }

            this.addReviwer = function(dom){
                var d = new dc(dom, reviewersDS);
                var data = d.exec();
                if( !!data.verify.key ){
                    dom.find('.error').removeClass('error');
                    data.verify.dom.addClass('error');
                    return;
                }

                var reviewer = data.data;
                if( !reviewer.affiliation )  reviewer.affiliation = '-';
                if( !reviewer.researchfield )  reviewer.researchfield = '-';

                _that.tmpReviewers.push(reviewer);
                _that.renderReviewers(_that.tmpReviewers, $('#reviewerList'))

                dom.find('.error').removeClass('error');
                dom.find('input').val('');
            }

            this.renderReviewers = function(users, dom){
                new _that.widget.getTable()(dom, tpl, users, function(body){
                    body.find('span.del').click(
                        function(){
                            var guid = $(this).closest('.row').attr('email');
                            _that.removeUser(users, guid, dom );
                        }
                    )
                }, "email");
            }

            this.removeUser = function(users, email, dom){
                var index = 0;
                for(; index<_that.reviewers.length;index++){
                    if( _that.reviewers[index].email == email ){
                        break;
                    }  
                }
                if( index < _that.reviewers.length ){
                    _that.reviewers.splice(index, 1);
                }
                _that.renderReviewers(_that.reviewers, dom);
            }   
            this.reviewerAddEnd = function(){
                _that.reviewers = _that.reviewers.concat(_that.tmpReviewers);
                _that.renderReviewers(_that.reviewers, $('#reviewers'));
                _that.tmpReviewers = [];
            }


            //=================add user ==================================
            var authors = [];
            var authorsDS = {
                'data-email': {
                    'verify': 'email',
                    'val' : ''
                },
                'data-name': {
                    'verify': 'nonull',
                    'val' : ''
                },
                'data-guid': {
                    'verify': '',
                    'val' : ''
                },
                'data-country': {
                    'verify': '',
                    'val' : ''
                },
                'data-affiliation': {
                    'verify': '',
                    'val' : ''
                },
                'data-corresponding': {
                    'verify': '',
                    'val' : ''
                }
            }

            this.analysAuthor = function(analysAuthors){
                _that.authors = [];
                var ds = new dc();
                analysAuthors.forEach(
                    function( d ){
                        _that.authors.push( { name: d, guid: ds.getGuid() } );
                    }
                );
                
                _that.renderAuthors(_that.authors);
            }

            this.newAuthor = function(){
                _that.authors.push({guid: new dc().getGuid()});
                _that.renderAuthors(_that.authors);
            }
            this.updateAuthors = function(guid, key, val){
                var index = 0;
                for(; index<_that.authors.length;index++){
                    if( _that.authors[index].guid == guid ){
                        break;
                    }  
                }
                _that.authors[index][key] = val;
                
            }

            this.userTpl = '{{ each list as a index }}<div style="display:flex" class="author" data="{{a.guid}}" order="{{index}}"><div class="cell"  style="text-align:center; font-size:14px; line-height:29px; flex: 1 1 5%" cell="5%" >{{index+1}}</div>'
                        +  '<div class="cell" style="flex: 1 1 19%" cell="19%" ><input value="{{a.name}}" name="name" placeholder="姓名"></div>'
                        +  '<div class="cell" style="flex: 1 1 19%" cell="19%"><input value="{{a.email}}" name="email" placeholder="邮箱"></div>'
                        +  '<div class="cell"  style="flex: 1 1 20%" cell="20%" ><input value="{{a.affiliation}}" name="affiliation" placeholder="单位"></div>'
                        +  '<div class="cell"  style="flex: 1 1 15%" cell="15%"><input value="{{a.country}}" name="country" placeholder="国家"></div>'
                        +  '<div class="cell"  style="flex: 1 1 14%; padding-left: 12px; padding-top:8px; " cell="14%"><span class="{{!!a.corresponding?"corresponding nocheck checked":"corresponding nocheck"}}"></div>'
                        +  '<div class="cell operation"  style="flex: 1 1 8%; " cell="8%"><span class="del"><i class="fa fa-close"></i></span> <span class="up"><i class="fa fa-arrow-up"></i></span> <span  class="down"><i class="fa fa-arrow-down"></i></span></div>'
                        + '</div>{{/each}}'
       
            this.renderAuthors = function(data){
                var users = {}; users.list = data;
                var dom = tpl.compile(_that.userTpl)(users);
                $('#authors .body').empty().append(dom);
                $('#authors .body').find('input').keyup(
                    function(){
                        var guid = $(this).closest(".author").attr('data');
                        var key = $(this).attr('name');
                        _that.updateAuthors(guid, key, $(this).val());
                    }
                );
                $('#authors .body').find('.corresponding').click(
                    function(){
                        if($(this).hasClass('checked')){
                            $(this).removeClass('checked');
                        }else{
                            $(this).addClass('checked');
                        }
                        var guid = $(this).closest(".author").attr('data');
                        _that.setCorresponing(guid, $(this).hasClass('checked'));
                    }
                )
                $('#authors .body').find('.operation span').click(
                    function(){
                        var op = $(this).attr('class');
                        switch(op){
                            case 'del':
                                var guid = $(this).closest('.author').attr('data');
                                _that.removeAuthor(guid);
                                $(this).closest('.author').remove();
                                break;
                            case 'up':
                                var index = $(this).closest('.author').attr('order');
                                _that.swapAuthor(index, 'up');
                                break;
                            case 'down':
                                var index = $(this).closest('.author').attr('order');
                                _that.swapAuthor(index, 'down');
                                break;
                            case 'down':
                                var index = $(this).closest('.author').attr('order');
                                _that.swapAuthor(index, 'down');
                                break;
                            
                        }
                    }
                )
            }
            this.setCorresponing = function( guid, flag ){
                var index = 0;
                for(; index<_that.authors.length;index++){
                    if( _that.authors[index].guid == guid ){
                        break;
                    }  
                }
                
                
                _that.authors[index].corresponding = flag;
            }

            this.swapAuthor = function(index, type){
                index = index * 1;
                if(( index == 0 && type=='up') ||(index==_that.authors.length-1&&type=='down')){
                    return;
                }else{
                    if( type == 'up'){
                        var tmp = _that.authors[index-1]; _that.authors[index-1]=_that.authors[index]; _that.authors[index]=tmp;
                    }else if(type == 'down'){
                        var tmp = _that.authors[index+1]; _that.authors[index+1]=_that.authors[index]; _that.authors[index]=tmp;
                    }
                }
                _that.renderAuthors(_that.authors);
            }

            this.removeAuthor = function(guid){
                var index = 0;
                for(; index<_that.authors.length;index++){
                    if( _that.authors[index].guid == guid ){
                        break;
                    }  
                }
                if( index < _that.authors.length ){
                    _that.authors.splice(index, 1);
                }
                console.log(_that.authors); 
                _that.renderAuthors(_that.authors);
            }

            this.verifyAuthors = function( data ){
                var d = new dc();
                for(var i=0; i<data.length; i++){
                    var key = d.execData(authorsDS, data[i]);
                    console.log(key);
                    if( !!key ){
                        return data[i].guid;
                    }
                }
                return null;
            }

            //=================upload file=================================
            this.uploadFiles = [];
            this.tmpUploadFiles = [];
            this.newUpload = function(){
                _that.tmpUploadFiles = [];
            }

            this.fileUploaded = function(){
                _that.uploadFiles = _that.uploadFiles.concat(_that.tmpUploadFiles);
                _that.renderFiles(_that.uploadFiles, $('#finalFiles'));
                _that.tmpUploadFiles = [];

                _that.uploadFiles.forEach(
                    function(d){
                        var file = _that.getPaper();
                        if( file != null ){
                            _that.ec.fire(
                                'submitdc',
                                'removeMastheadMask',
                                file
                            )
                        }
                    }
                )
            }

            this.checkFileTypeRepeat = function( fileArr, file ){
                for(var i=0; i<fileArr.length; i++){
                    if( (file.filetype == fileArr[i].filetype) && file.filetype.indexOf("Files") == -1 ){
                        return false;
                    }
                }
                return true;
            }

            this.addNewFile = function( file ){
                var d = new dc();
                file.guid = d.getGuid();
                //file.id = _that.uploadFiles.length + 1;
                //防止上传文件重复
                if( !_that.checkFileTypeRepeat(_that.tmpUploadFiles, file) 
                    ||!_that.checkFileTypeRepeat(_that.uploadFiles, file) 
                ){
                    _that.ec.fire(
                        'submitdc',
                        'uploadFileRepeat',
                        null
                    )
                    return;
                }

                _that.tmpUploadFiles.push(file);
                _that.renderFiles(_that.tmpUploadFiles, $('#uploadfiles'))
            }

            this.renderFiles = function(files, dom){
                new _that.widget.getTable()(dom, tpl, files, function(body){
                    body.find('span.del').click(
                        function(){
                            var guid = $(this).closest('.row').attr('guid');
                            _that.removeFile(files, guid, dom );
                        }
                    )
                }, "guid");
              
            }

            this.removeFile = function(files, guid, dom){
                var index = 0;
                for(var i=0; i<files.length; i++ ){
                    if( files[i].guid == guid ){
                        index = i;
                        break;
                    }
                }

                if( index < files.length){
                    files.splice(index,1);
                }
                _that.renderFiles(files, dom);
                
            }
            //===============SAVE CACHE==============================
            this.saveCache = function( c ){
                var data = {};
                data.acceptFlag = _that.acceptFlag;
                data.sectionId = _that.sectionId;
                data.sectionText = _that.sectionText;
                data.reviewers = _that.reviewers;
                data.authors = _that.authors;
                data.uploadFiles = _that.uploadFiles;

                $.extend(data, c);
                store.set('infos', data);

            }

            this.loadCache = function( ed ){
                var data = store.get('infos');
                
                if(!!data){
                    _that.acceptFlag  = (!!data.acceptFlag ? data.acceptFlag : false);
                    _that.sectionId =  (!!data.sectionId? data.sectionId : null);
                    _that.sectionText = (!!data.sectionText?data.sectionText : null);
                    _that.reviewers = (!!data.reviewers? data.reviewers : []);
                    _that.authors  =  (!!data.authors? data.authors : []);
                    _that.uploadFiles  =  (!!data.uploadFiles ? data.uploadFiles : []);
                }else{
                    return null;
                } 

                if(data.acceptFlag) $('#accept').addClass('checked');
                if(!!data.sectionId) {
                    $('#sectionslist input').val(data.sectionText);
                }

                if(!!data.uploadFiles) {
                    _that.renderFiles(data.uploadFiles, $('#finalFiles'));
                }

                if(!!data.reviewers) {
                    _that.renderReviewers(data.reviewers, $('#reviewers'));
                }

                if(!!data.authors) {
                    _that.renderAuthors(data.authors);
                }

                if(!!data.title){
                    $('[data-title]').val(data.title);
                }

                if(!!data.keywords){
                    $('[data-keywords]').val(data.keywords);
                }

                if(!!data.abstract){
                    ed.set(data.abstract);
                }


                if( data == null ){
                    return null;
                }else{
                    return data;
                }
            }

            this.clearCache = function(){
                store.set('infos', null);
                window.location.reload();
            }

            //==================submit=====================================
            this.submit = function(c){
                var data = {};
                //校验作者
                var flag = _that.verifyAuthors(_that.authors);
                if(!!flag){
                    _that.ec.fire(
                        'submitdc',
                        'authorVerifyFailed',
                        flag //没有通过的作者GUID
                    )
                    return;
                }
                //添加作者顺序
                var index = 0;
                for(; index<_that.authors.length;index++){
                    _that.authors[index]['order'] = index;
                }
                data.acceptFlag = _that.acceptFlag;
                data.sectionId = _that.sectionId;
                data.sectionText = _that.sectionText;
                data.reviewers = _that.reviewers;
                data.authors = _that.authors;
                data.uploadFiles = _that.uploadFiles;

                $.extend(data, c);
                _that.ec.fire(
                    'submitdc',
                    'submitCheck',
                    data
                )
            }            

            this.exports = {
                'newUpload': this.newUpload,
                'fileuploaded' : this.addNewFile,
                'accept': this.accept,
                'fileUploadEnd' : this.fileUploaded,

                'analysAuthor': this.analysAuthor,
                'addNewAuthor': this.newAuthor,
                'addUser' : this.addUser,
                'userAddEnd' : this.userAddEnd,
                
                'newReviwer': this.newReviwer,
                'addReviwer': this.addReviwer,
                'reviewerAddEnd' : this.reviewerAddEnd,
                'sectionSelect' :　this.sectionSelect,
                'savecache' : this.saveCache,
                'loadCache' : this.loadCache,
                'clearCache' : this.clearCache,
                'finalsubmit': this.submit
            }
        }
        return SumbitDc;
})