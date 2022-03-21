package org.poseestimation.data

import org.json.JSONArray
import org.json.JSONObject


class ExerciseSchedule(JSONmeg:String) {

    private val meg:String=JSONmeg
    public var exerciseName:MutableList<String> = arrayListOf<String>()
    public var exerciseId:MutableList<Int> = arrayListOf<Int>()
    public var exerciseGroups:MutableList<Int> = arrayListOf<Int>()
    public var tags:MutableList<MutableList<Int>> = arrayListOf<MutableList<Int>>()
    init{
        JSON_decoder()
    }
    private fun JSON_decoder()
    {
        var jsonObj=JSONObject(meg)
        var dataArray=jsonObj.getJSONArray("data")
        for(i in 0..dataArray.length()-1)
        {
            exerciseName.add(dataArray.getJSONObject(i).get("sv_path").toString())
            exerciseId.add(dataArray.getJSONObject(0).get("id").toString().toInt())
            exerciseGroups.add( dataArray.getJSONObject(0).get("groups").toString().toInt())
        }

        val Tagstr:String="{\n" +
                "    \"item\": [[5,5,5,5,5,5,5,5,5],\n" +
                "            [1,2,3,2,5,7,7,7,10],\n" +
                "            [7,3,3,7,8,6,3,6,10],\n" +
                "            [8,8,8,10,2,2,2,2,1],\n" +
                "            [5,5,8,10,0,0,0,0,1],\n" +
                "            [5,5,8,10,0,0,0,0,1],\n" +
                "            [5,5,8,10,0,0,0,0,1],\n" +
                "            [1,2,10,6,2,3,0,0,1],\n" +
                "            [4,8,10,4,1,8,0,0,1],\n" +
                "            [4,8,10,4,3,8,0,4,1],\n" +
                "            [2,8,10,4,1,1,0,0,1],\n" +
                "            [10,7,5,8,7,4,1,1,4],\n" +
                "            [10,7,5,8,4,2,1,1,3],\n" +
                "            [10,7,5,8,4,2,1,1,3],\n" +
                "            [2,4,1,1,8,6,1,1,3],\n" +
                "            [2,4,1,1,8,6,8,1,3],\n" +
                "            [1,5,2,1,6,10,4,1,3],\n" +
                "            [2,6,2,1,8,8,2,1,3],\n" +
                "            [6,5,1,5,6,8,2,1,3],\n" +
                "            [1,6,1,2,9,8,2,1,3],\n" +
                "            [1,6,1,1,7,8,5,5,3],\n" +
                "            [1,6,1,1,7,8,5,5,3],\n" +
                "            [1,1,1,1,2,2,10,10,5],\n" +
                "            [1,1,1,1,2,2,9,8,5],\n" +
                "            [1,1,1,1,2,2,9,8,5],\n" +
                "            [1,1,1,1,2,2,9,7,4],\n" +
                "            [1,1,1,1,2,2,9,7,4],\n" +
                "            [1,1,1,1,5,4,9,9,4],\n" +
                "            [1,1,1,1,3,5,7,6,2],\n" +
                "            [1,1,1,1,3,5,7,6,2]]\n" +
                "}"
        var TagObj=JSONObject(Tagstr)
        var TagMatrix = TagObj.getJSONArray("item")
        for(i in 0..29)
        {
            var dataArray=TagMatrix.getJSONArray(i)
            var temp:MutableList<Int> = arrayListOf()
            for(j in 0..8)
            {
                temp.add(dataArray.getInt(j))
            }
            tags.add(temp)
        }

    }
}
