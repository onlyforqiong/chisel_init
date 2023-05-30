package examples


import chisel3._
import chisel3.stage._
import chisel3.util._


class Vga extends BlackBox  {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Reset())
    val vga_data = Input(UInt(12.W))
    val h_addr = Output(UInt(10.W))
    val v_addr = Output(UInt(10.W))
    val hsync = Output(Bool())
    val vsync = Output(Bool())
    val valid = Output(Bool())
    val vga_r = Output(UInt(4.W))
    val vga_g = Output(UInt(4.W))
    val vga_b = Output(UInt(4.W))
  })
}


class KeyboardBottom extends BlackBox  {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Reset())
    val ps2_clk = Input(Bool())
    val ps2_data = Input(Bool())
    val continue_flag = Output(Bool())
    val keyboard_data = Output(UInt(8.W))
    val loosen_flag = Output(Bool())
    val ready = Output(Bool())
  })
}
import scala.io.Source
class FileReader(filename: String) extends Module {
  val io = IO(new Bundle {
    // val filename = Input(String())
    val filedata = Output(Vec(10, UInt(8.W)))
  })
  
  // Read the data from the file using Scala's built-in file I/O functions

  val lines = Source.fromFile(filename).getLines().toArray
  val data = lines.map(line => line.toInt.asUInt)

  // Convert the data to a Vec of UInts
  val filedata_vec = VecInit(data.take(10))
  io.filedata := filedata_vec
}

// Example usage:


class cmd_line extends Module {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Reset())
    val ps2_clk = Input(Bool())
    val ps2_data = Input(Bool())
    val vga_vsync = Output(Bool())
    val vga_hsync = Output(Bool())
    val vga_red = Output(UInt(4.W))
    val vga_green = Output(UInt(4.W))
    val vga_blue = Output(UInt(4.W))
    val addr_h = Output(UInt(10.W))
    val addr_v = Output(UInt(10.W))
    val led = Output(Bool())
  })
    val reader = Module(new FileReader("/media/ddddddd/ddddddd/lesson_learning/nvboard/example/resource/picture.hex"))
    val read_data = reader.io.filedata
    

}