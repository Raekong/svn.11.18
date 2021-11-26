define(
    [ "dataverifier" ],
	function( dv ){
        function DataCenter( container, dataStructure ){
            this.container = container;
            this.dataStructure = dataStructure;
            this.dataResult = {};
            this.verifyResult = {};

            var _that = this;

            // 
            //  dataStructure: 
            //  'elem-***' :  {  //for a dataelement define
            //      'verify': for binded input value verify : email| no null | int
            //      'vmsg' : for verify failed msg, optional
            //      'val' : default value
            //  }
            // getFlag is a obj, 
            //      {
            //         flag : whether value verify is passed, false for failed
            //         msg : if failed, the return alert msg  
            //      }            
            // //

            //在使用DC之前必须要复位
            this.exec = function(){
                var dataResult = {};
                var verifyResult = {};
               
                this.get(this.dataStructure, dataResult, verifyResult );
                return { data: dataResult, verify: verifyResult };
                
            }

            this.execData = function(ds, data){
                console.log( data );
                for(var key in ds){
                    var val = data[key.split('-')[1]];
                    var item = ds[key];
                    
                    if(  item['verify'] != ''){
                        if(!dv.verify( val, item['verify'] )){
                            return key;
                        }
                    }
                }
                return null;
            }

            this.getGuid = function(){
                return 'xyxxyxy'.replace(/[xy]/g,function(c){
                    var r = Math.random()*16|0,
                    v = c == 'x' ? r :(r&0x3|0x8);
                    return v.toString(16);
                });
            }
            
            this.get = function( dataStructure, dataResult, verifyResult ){ //obj是DC中的数据模板，有上例的数据结构，真正的数据项用elem前缀
                var obj = dataStructure;
               
                for( var key in obj){
                   
                    if( dv.isObject(obj[key]) && !verifyResult.key ){ //校验无误且校验值是一个对象，进入下层
                        if( key.indexOf('data-') == -1 ){//递归进入获取数据
                            dataResult[key] = {};
                            _that.get( obj[key], dataResult[key], verifyResult, flag ); 
                        }else{
                            
                            //找到具体的数据项
                            var real_key = key.substring( key.indexOf("data-")+5 );
                            var dom = _that.container.find('[data-'+ real_key +']');    //DOM中数据项以data-前缀的属性为标识

                            var val = null;
                            console.log('[data-'+ real_key +']');
                            if(!dom[0]) return;
                            if( dom[0].tagName.toLowerCase() == 'input' ){
                                val = dom.val();
                                if( !val ) val = dataStructure[key].val.trim(); // for default value;
                            }else{
                                val = dom.attr('data-'+ real_key +'');
                            }

                            dataResult[real_key] = val;
                            if( !!obj[key]['verify'] ){
                               if( !dv.verify( val, obj[key]['verify'] )) { //数据校验，只要有一项不对，就返回一项
                                    verifyResult.dom = dom;
                                    verifyResult.key = key;
                               }
                            }
                        }
                    }
                }
            }

            this.initCheck = function( obj ){
                for( var key in obj){
                    if( dv.isObject(obj[key]) && key.indexOf('elem-') == -1 ){
                        return _that.initCheck( obj[key] );
                    }else{
                        key = key.substring( key.indexOf("elem-")+5 );
                        var dom = _that.container.find('[data-'+ key +']');
                        if(!dom || dom.length === 0){
                            console.log( 'the dom of define data key : data-' +  key  + ' isn\'t  found !');
                        }
                    }
                }
            }

            this.init = function(){
                this.initCheck( this.dataStructure );
            }

            function DataResult( data ) {
                this.data = data;

                this.getVerifyRst = function(  vrst ){
                    if(!dv.isObject( vrst )) return true;
                    
                    if( typeof vrst.veriflag != 'undefined'){
                        return vrst;
                    }else{
                        for( var key in vrst) {
                            var v = this.getVerifyRst( vrst[key]) ;
                            if( dv.isObject( v ) ) return v;
                        }
                    }                    
                    return true;
                }

                this.verifyRst = function(){
                    return this.getVerifyRst(this.data);
                }

                this.getData = function(){
                    return this.data;
                }
            }

            this.getData = function(){  
                var rst = this.get( this.dataStructure, {}  );
                return  new DataResult(rst);
            }
        }
        return DataCenter;
    })