
function dynamicAddUser(num, playerList){
    document.getElementById("divTable").style.display="";
    // 每次刷新匹配大厅的用户信息时，删除之前的表格数据
    $("#myTable").empty();
    for(var i=1;i<=num;i++)
    {
        var trNode=document.createElement("tr");
        $(trNode).attr("align","center");
        //序号
        var tdNodeNum=document.createElement("td");
        $(tdNodeNum).html(i);
        tdNodeNum.style.width = "150px";
        tdNodeNum.style.height = "33px";
        tdNodeNum.className = "td2";
        //用户名
        var tdNodeName=document.createElement("td");
        $(tdNodeName).attr("id", "opponent").html(playerList[i-1].playerEthAddress);
        tdNodeName.style.width = "300px";
        tdNodeName.className = "td2";
        //赌注
        var tdNodePri=document.createElement("td");
        tdNodePri.style.width = "250px";
        tdNodePri.className = "td2";
        var priText=document.createElement("span");
        $(priText).attr("id", "opponentBit").css({"display":"inline-block","text-axlign":"center"});
        $(priText).text(playerList[i-1].bit + "eth");
        tdNodePri.appendChild(priText);
        //操作
        var tdNodeOper = document.createElement("td");
        tdNodeOper.style.width = "170px";
        tdNodeOper.className = "td2";
        var editA = document.createElement("a");
        // var editA = document.createElement("button");
        $(editA).attr("href", "javascript:void(0);").attr("onclick", "matchUser(this)").text("匹配");
        // $(editA).attr("id", "matchOpponent").text("匹配");
        // $('#matchOpponent').click(() => {
        //     let bit = $(this).parents("tr").find("opponentBit").text();
        //     let opponent = $(this).parents("tr").find("opponent").text();
        //     console.log(bit);
        //     console.log(opponent);
        // });
        $(editA).css({ display: "inline-block" });
        tdNodeOper.appendChild(editA);

        trNode.appendChild(tdNodeNum);
        trNode.appendChild(tdNodeName);
        trNode.appendChild(tdNodePri);
        trNode.appendChild(tdNodeOper);
        $("#myTable")[0].appendChild(trNode);
    }
}


/**
 * 分页函数
 * pno--页数
 * psize--每页显示记录数
 * 分页部分是从真实数据行开始，因而存在加减某个常数，以确定真正的记录数
 * 纯js分页实质是数据行全部加载，通过是否显示属性完成分页功能
 **/
var pageSize=10;//每页显示行数
var currentPage_=1;//当前页全局变量，用于跳转时判断是否在相同页，在就不跳，否则跳转。
var totalPage;//总页数
function goPage(pno,psize){
    var itable = document.getElementById("myTable");
    var num = itable.rows.length;//表格所有行数(所有记录数)

    pageSize = psize;//每页显示行数
    //总共分几页
    if(num/pageSize > parseInt(num/pageSize)){
        totalPage=parseInt(num/pageSize)+1;
    }else{
        totalPage=parseInt(num/pageSize);
    }
    var currentPage = pno;//当前页数
    currentPage_=currentPage;
    var startRow = (currentPage - 1) * pageSize+1;
    var endRow = currentPage * pageSize;
    endRow = (endRow > num)? num : endRow;

    $("#myTable tr").hide();
    for(var i=startRow-1;i<endRow;i++) {
        $("#myTable tr").eq(i).show();
    }

    var tempStr = currentPage+"/"+totalPage;
    document.getElementById("barcon1").innerHTML = tempStr;

    if(currentPage>1){
        $("#firstPage").on("click",function(){
            goPage(1,psize);
        }).removeClass("ban");
        $("#prePage").on("click",function(){
            goPage(currentPage-1,psize);
        }).removeClass("ban");
    }else{
        $("#firstPage").off("click").addClass("ban");
        $("#prePage").off("click").addClass("ban");
    }

    if(currentPage<totalPage){
        $("#nextPage").on("click",function(){
            goPage(currentPage+1,psize);
        }).removeClass("ban")
        $("#lastPage").on("click",function(){
            goPage(totalPage,psize);
        }).removeClass("ban")
    }else{
        $("#nextPage").off("click").addClass("ban");
        $("#lastPage").off("click").addClass("ban");
    }
}

function jumpPage() {
    var num=parseInt($("#num").val());
    if(num != currentPage_ && !isNaN(num) && num <= totalPage && num > 0) {
        goPage(num,pageSize);
    }
}


