define(
    ['layui'], 
    function( ){
       var layer = layui.layer;
       layer.config({
          path: '/js/third-party/layui/lay/modules'
      })
    
       var msg = function( title, tip, btns, callback ){
            var btns = btns.split(',')
            layer.alert(
                tip,{
                    btn: btns,
                    closeBtn: 0
                    ,title: title
                  }, callback
            );
        }


        return {
            msg : msg,
            close: layer.close,
            layer: layer,
            date: layui.laydate
        };
            
})