package examples

import chisel3._
import chisel3.stage._
import chisel3.util._
import dataclass.data

class vga(data_width:Int) extends  BlackBox{
    val io = IO(new Bundle {
        val clk = Input(Bool())
        val rst = Input(Bool())
        val vga_data = Input(UInt((data_width * 3).W))
        val hsync = Output(Bool())
        val vsync = Output(Bool())
        val valid = Output(Bool())
        val vga_r = Output(UInt(data_width.W))
        val vga_g = Output(UInt(data_width.W))
        val vga_b = Output(UInt(data_width.W))
    })
}
class clk_converter extends BlackBox {
    val io = IO(new Bundle {
        val clk_in1 = Input(Bool())
        val clk_out1 = Output(Bool())
    })
}

class vga_module(data_width:Int) extends  Module {
    val vga_vsync = Output(Bool())
    val vga_hsync = Output(Bool())
    val vga_red = Output(UInt(data_width.W))
    val vga_green = Output(UInt(data_width.W))
    val vga_blue = Output(UInt(data_width.W))
    val clk_con = Module(new clk_converter)
    clk_con.io.clk_in1 := clock.asBool
    withClock(clk_con.io.clk_out1.asClock) {
        val vga_controler = Module(new vga(data_width))
         
    } 
}