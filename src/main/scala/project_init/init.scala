package examples

import chisel3._
import chisel3.stage._
import chisel3.util._


class segment_decoder extends Module{
    val io = IO(new Bundle {
        val in = Input(UInt(4.W))
        val out = Output(UInt(8.W))
    })
    io.out := ~(MuxLookup(io.in,0.U,Seq(
		0.U -> 0x3f.U,
        1.U -> 0x06.U,
        2.U -> 0x5b.U,
		3.U -> 0x4f.U,
        4.U -> 0x66.U,
        5.U -> 0x6d.U,
		6.U -> 0x7d.U,
        7.U -> 0x07.U,
        8.U -> 0x7f.U,
		9.U -> 0x6f.U )))
}
class segment_shine(sum_num:Int) extends  Module{
    val io = IO(new Bundle {
        val en = Input(Bool())
        val sel = Vec(sum_num,Output(Bool()))
        val data_in = Vec(sum_num,Input(UInt(4.W))) // 显示的数值输入，最多到f
        val seg_line = Output(UInt(8.W))
    })
    //先分频 
    val (counter,div_signal) = Counter(io.en,1000)
    val (counter_shine,b) = Counter(io.en && div_signal,sum_num)
    // io.seg_line := MuxLookup()
    io.sel.zipWithIndex.foreach{case(a,index) => 
        io.sel(index) := !(index.U === counter_shine)    
    }
    val decoder =  VecInit(Seq.fill(sum_num)(Module(new segment_decoder).io))
    decoder.zipWithIndex.foreach{case(a,index) => 
        decoder(index).in := io.data_in(index)
    }
    io.seg_line := decoder(counter_shine).out

}

class led_shine(sum_counter:Int,set_max:Array[Int]) extends Module  {
        
    val io = IO(new Bundle { 

        val led  = Vec(sum_counter,Output(Bool()))
        val en = Input(Bool())
        val sel = Vec(4,Output(Bool()))
        val seg_line = Output(UInt(8.W))
        
        val bottom = Vec(4,Input(Bool()))

    })
    val (counter, signal) = Counter(1.U.asBool,10000000)
    val (counter_sum ,b) = Counter(1.U.asBool && signal,sum_counter)
    io.led.zipWithIndex.foreach{case(a,index) => 
        io.led(index) := index.U === counter_sum
    }

    val segment = Module(new segment_shine(4)).io
    io.seg_line := segment.seg_line
    io.sel := segment.sel
    val (div_sec_counter,div_sec_signal) = Counter(1.U.asBool,10000000)

    val way_control_reg_0 = RegInit(0.U(32.W))
    val way_control_reg_1 = RegInit(set_max(1).U(32.W))

    val counter_finish_signal = Wire(Vec(2,(Bool())))
    
    // counter_finish_signal.zipWithIndex.foreach{case (a,index) => counter_finish_signal(index) := way_control_reg(index) === 0.U}
    counter_finish_signal(0) := way_control_reg_0 === 0.U
    counter_finish_signal(1) := way_control_reg_1 === 0.U
    val counter_doing = RegInit(VecInit(Seq.fill(2)(0.U.asBool)))
    counter_doing.zipWithIndex.foreach{case(a,index) => 
            counter_doing(index) := Mux(div_sec_signal,if(index == 0) way_control_reg_0 else way_control_reg_1,counter_doing(index))
    }

    way_control_reg_0 := Mux(io.en,Mux(div_sec_signal,Mux(way_control_reg_0 === 0.U ,Mux(counter_finish_signal(1) && counter_doing(0) === 0.U,set_max(0).asUInt,0.U),way_control_reg_0 - 1.U),
        way_control_reg_0),Mux(div_sec_signal && io.bottom(0),Mux(way_control_reg_0 === 0.U,set_max(0).asUInt,way_control_reg_0 - 1.U),
        way_control_reg_0))

    way_control_reg_1 := Mux(io.en,Mux(div_sec_signal,Mux(way_control_reg_1 === 0.U,Mux(counter_finish_signal(0) && counter_doing(1) === 0.U,set_max(1).asUInt,0.U),way_control_reg_1 - 1.U),
        way_control_reg_1),Mux(div_sec_signal && io.bottom(1),Mux(way_control_reg_1 === 0.U,set_max(1).asUInt,way_control_reg_1 - 1.U),
        way_control_reg_1))
    
    // val sec_counter_small = RegInit(0.U(32.W))
    // val sec_counter_large = RegInit(0.U(32.W))

    // val min_counter_small = RegInit(0.U(32.W))
    // val min_counter_large = RegInit(0.U(32.W))
    

    // sec_counter_small := Mux(io.en && div_sec_signal,Mux(sec_counter_small === (10 - 1).U,0.U,(sec_counter_small + 1.U)),
    //     Mux(!io.en && div_sec_signal && io.bottom(0) ,Mux(sec_counter_small === (10 - 1).U,0.U,(sec_counter_small + 1.U)),sec_counter_small)) 
    // val sec_signal_small = io.en && div_sec_signal && sec_counter_small === (10 - 1).U

    // sec_counter_large := Mux(io.en && sec_signal_small,Mux(sec_counter_large === (6 - 1).U,0.U,(sec_counter_large + 1.U)),
    //     Mux(!io.en && div_sec_signal && io.bottom(1) ,Mux(sec_counter_large === (6 - 1).U,0.U,(sec_counter_large + 1.U)),sec_counter_large)) 
    //  val sec_signal_large = io.en && sec_signal_small && sec_counter_large === (6 - 1).U

    // min_counter_small := Mux(io.en && sec_signal_large,Mux(min_counter_small === (10 - 1).U,0.U,(min_counter_small + 1.U)),
    //     Mux(!io.en && div_sec_signal && io.bottom(2) ,Mux(min_counter_small === (10 - 1).U,0.U,(min_counter_small + 1.U)),min_counter_small)) 
    // val min_signal_small = io.en && sec_signal_large && min_counter_small === (10 - 1).U
    
    // min_counter_large := Mux(io.en && min_signal_small,Mux(min_counter_large === (6 - 1).U,0.U,(min_counter_large + 1.U)),
    //     Mux(!io.en && div_sec_signal && io.bottom(3) ,Mux(min_counter_large === (6 - 1).U,0.U,(min_counter_large + 1.U)),min_counter_large)) 
    //  val min_signal_large = io.en && min_signal_small && min_counter_large === (6 - 1).U
    // // val (sec_counter_small,sec_signal_small) = Counter(io.en && div_sec_signal,10)
    // // val (sec_counter_large,sec_signal_large) = Counter(io.en && sec_signal_small,6)

    // // val (min_counter_small,min_signal_small) = Counter(io.en && sec_signal_large,10)
    // // val (min_counter_large,min_signal_large) = Counter(io.en && min_signal_small,6)
    
    segment.data_in(0) := way_control_reg_0%10.U;
    segment.data_in(1) := way_control_reg_0/10.U;
    segment.data_in(2) := way_control_reg_1%10.U;
    segment.data_in(3) := way_control_reg_1/10.U;
    
    segment.en := 1.U.asBool

    
}

// object seg_test extends App{
//     (new ChiselStage).emitVerilog(new segment_shine(4))
// }