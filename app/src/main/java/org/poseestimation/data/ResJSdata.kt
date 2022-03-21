package org.poseestimation.data

import Jama.Matrix
import org.json.JSONArray
import org.json.JSONObject
import org.poseestimation.utils.DTWprocess

class ResJSdata {

    //用户动作骨架
    private lateinit var Vectorslist:MutableList<Matrix>
//Respond
    public var count=0
    //动作标准程度
    private lateinit var scoreBypart:MutableList<MutableList<Double>>
    //完成度
    private lateinit var completeness:MutableList<Boolean>
    //心率
    private lateinit var beatRate:MutableList<Int>
    //运动强度
    private lateinit var exerciseIntensity:MutableList<Double>
    //DTW
    private lateinit var dtwres:Dtwresponse

    init {
        Vectorslist = mutableListOf<Matrix>()
        scoreBypart = mutableListOf<MutableList<Double>>()
        completeness = mutableListOf<Boolean>()
        exerciseIntensity = mutableListOf<Double>()
    }
    fun append(obj1 :MutableList<Double>,obj2:Matrix)
    {
        //obj1.size为0表示缺失
        scoreBypart.add(obj1)
        Vectorslist.add(obj2)
        if(obj1.count()==0)
            completeness.add(false)//缺失
        else
            completeness.add(true)//非缺失
        count++
    }

    fun exec(sampleVectorList: MutableList<Matrix>)
    {
        exerciseIntensity=arrayListOf<Double>()
        for(i in 1..count-1)
        {
            exerciseIntensity.add(Vectorslist.get(i).minus(Vectorslist.get(i-1)).norm2())
        }
        dtwres=DTWprocess().exec_Jama(this.Vectorslist,sampleVectorList)

        dtwres?.DTW_matrix
        dtwres?.score
    }

    fun toJson():JSONObject
    {
        var ALLresobj:JSONObject = JSONObject()//最终的返回数据

        var DTWresobj:JSONObject = JSONObject()//DTW的返回数据
        var DTWscore:JSONArray = JSONArray()//DTW的返回数据
        var DTWmatrix:JSONArray = JSONArray()//DTW的返回数据

        var CPNresArray:JSONArray = JSONArray()//完成度的返回数据
        var EXTresArray:JSONArray = JSONArray()//运动强度的返回数据

        var SCOresArray:JSONArray = JSONArray()//运动分数的返回数据
        val sampleNum=dtwres.DTW_matrix[0]?.count()

        for(i in 0..count-1)
        {
            //获得完成度
            CPNresArray.put(completeness[i])

            //获得运动强度，size-1
            if(i!=count-1)
                EXTresArray.put(exerciseIntensity[i])

            //获得动作分数
            var SCOline:JSONArray = JSONArray()
            for(j in 0..10)
            {
                SCOline.put(scoreBypart[i][j])
            }
            SCOresArray.put(SCOline)

            //获得DTW矩阵每行
            var DTWline:JSONArray = JSONArray()
            for(j in 0..sampleNum-1)
                DTWline.put(dtwres.DTW_matrix[i][j])
            DTWmatrix.put(DTWline)

        }

        DTWscore.put(dtwres.score)
        DTWresobj.put("matrix",DTWmatrix)
        DTWresobj.put("score",DTWscore)

        ALLresobj.put("scorebypart",SCOresArray)
        ALLresobj.put("completeness",CPNresArray)
        ALLresobj.put("exerciseIntensity",EXTresArray)
        ALLresobj.put("DTW",DTWresobj)

        return ALLresobj

    }

}