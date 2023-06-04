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

import chisel3._
import chisel3.util._
import scala.io.Source

class CommandLineTest extends Module {
  val io = IO(new Bundle {
    val ps2_clk = Input(Bool())
    val ps2_data = Input(Bool())
    val vga_vs = Output(Bool())
    val vga_hs = Output(Bool())

    val vga_blank_n = Output(Bool())
    val vga_sync_n = Output(Bool())
    val vga_r = Output(UInt(8.W))
    val vga_g = Output(UInt(8.W))
    val vga_b = Output(UInt(8.W))
    val addr_h = Output(UInt(10.W))
    val addr_v = Output(UInt(10.W))
    val led = Output(UInt(1.W))
  })

  val rawTableData = Mem(256, UInt(16.W))
  val lookupTable = Mem(256, UInt(8.W))
  val vgaDataArray = Mem(500001, UInt(24.W))
  val charTable = Mem(5001, UInt(12.W))
  val screenData = Mem(2501, UInt(8.W))

  // Initialize lookupTable from rawTableData
  for (i <- 0 until 256) {
    lookupTable.write(rawTableData(i)(7, 0), rawTableData(i)(15, 8))
  }
  io.led := 1.U.asBool

  // Load memory contents from files
  // $readmemh is not supported in Chisel, so use Scala's io.Source instead
  import chisel3.util.experimental.loadMemoryFromFile
  import firrtl.annotations.MemoryLoadFileType

  loadMemoryFromFile(vgaDataArray, "/home/ddddddd/my_learn/cpu_relative/fpga_sopc_test/resource/picture.hex", MemoryLoadFileType.Hex)
  // val pictureFile = Source.fromFile("/home/ddddddd/my_learn/cpu_relative/fpga_sopc_test/resource/picture.hex")
  // for ((line, i) <- pictureFile.getLines().zipWithIndex) {
    // vgaDataArray.write(i.U, line.U)
  // }
  // pictureFile.close()

  val glyphFile = Source.fromFile("/home/ddddddd/my_learn/cpu_relative/fpga_sopc_test/resource/vga_font.txt")
  // glyphFile.report()
  print(glyphFile)
  
  for ((line, i) <- glyphFile.getLines().zipWithIndex) {
    // print("i is " ,i)
    // print("\n")
    val decimalValue = Integer.parseInt(line.trim, 16) // 转换为十进制整数
    charTable.write(i.U, decimalValue.U)
  }
  glyphFile.close()

  // VGA module
  val vgaModule = Module(new Vga)
  vgaModule.io.clk := clock
  vgaModule.io.rst := reset
  vgaModule.io.vga_data := vgaDataArray(Cat(io.addr_h, io.addr_v(8, 0)))
  io.vga_hs := vgaModule.io.hsync
//   io.vgaOops, it seems my previous response got cut off. Here's the rest of the implementation for the `CommandLineTest` module in Chisel:

// ```scala
  io.vga_vs := vgaModule.io.vsync
  io.vga_blank_n := vgaModule.io.valid
  io.vga_sync_n := 1.U // Always sync on negative edge
  io.vga_r := vgaModule.io.vga_r
  io.vga_g := vgaModule.io.vga_g
  io.vga_b := vgaModule.io.vga_b
  io.addr_h := vgaModule.io.h_addr
  io.addr_v := vgaModule.io.v_addr
  // io.vga_clk := vgaModule.io.

  // Keyboard module
  val keyboardModule = Module(new KeyboardBottom)
  keyboardModule.io.clk :=  clock
  keyboardModule.io.rst := reset
  keyboardModule.io.ps2_clk := io.ps2_clk
  keyboardModule.io.ps2_data := io.ps2_data
  val keyBoardDataSync = RegInit(0.U(24.W))
  keyBoardDataSync := Cat(keyBoardDataSync(15, 0), keyboardModule.io.keyboard_data)
  val loosenFlag = RegInit(false.B)
  loosenFlag := keyboardModule.io.loosen_flag
  val ready = RegInit(false.B)
  ready := keyboardModule.io.ready

  // Command line processing
  val commandX = RegInit(0.U(8.W))
  val commandY = RegInit(0.U(8.W))
  val tempAsc = RegInit(0.U(8.W))
  val downSet = RegInit(0.U(12.W))

  when (reset.asBool()) {
    commandX := 0.U
    commandY := 0.U
    tempAsc := 0.U
    downSet := 0.U
  } .otherwise {
    when (ready) {
      when (loosenFlag || keyBoardDataSync(15, 8) === 0xF0.U) {
        commandX := commandX
        commandY := commandY
      } .otherwise {
        // switch (keyBoardDataSync(7, 0)) {
          when(keyBoardDataSync(7, 0) ===  0x5A.U) { // Enter key
            commandX := 0.U
            commandY := commandY + 1.U
          }
          .otherwise { // Other keys, fill in the blanks
            // $display("key data is %h and table answer is %h", keyBoardDataSync(7, 0), lookupTable(keyBoardDataSync(7, 0)))
            screenData(commandX + commandY * 70.U) := lookupTable(keyBoardDataSync(7, 0))
            when (commandX < 70.U) {
              commandX := commandX + 1.U
            } .otherwise {
              commandX := 0.U
              commandY := commandY + 1.U
            }
        }
      }
    }

    // VGA data processing
    for (tempV <- 1 until 481) {
      val charY = tempV / 16
      for (tempH <- 1 until 681) {
        print(tempH)
        print("\n")
        val charX = tempH / 9
        downSet := (charX + charY * 70).U
        tempAsc := screenData(downSet)
        vgaDataArray(tempH + (tempV << 10)) := Mux(charTable(tempAsc << 4 | ((tempV - 1) % 16).U)(tempH - 1), 0xFFFFFF.U, 0.U)
      }
    }
  }

  // Output LED based on commandX
  io.led := commandX(0)

  // Debugging output
  // $display("commandX = %h, commandY = %h, tempAsc = %h, downSet = %h", commandX, commandY, tempAsc, downSet)
}

object cmd_test extends App{
    (new ChiselStage).emitVerilog(new CommandLineTest)
}