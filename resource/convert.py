import os
# 打开MIF文件
mif_file = open("scancode.mif", "r")

# 读取MIF文件内容
mif_content = mif_file.read()

# 关闭MIF文件
mif_file.close()

# 将MIF内容转换为COE格式
coe_content = "memory_initialization_radix=16;\nmemory_initialization_vector=\n"

# 解析MIF文件内容
for line in mif_content.split("\n"):
    if ":" in line:
        # 获取地址和数据
        address, data = line.split(":")
        address = address.strip()
        data = data.strip()

        # 处理地址范围
        if ".." in address:
            start, end = address.split("..")
            print(start)
            start =  start[1:]
            start = int(start, 16)
            end = end.rstrip("]")
            end = int(end, 16)
            data = "00" * ((end - start + 1) * 2)

        # 将数据转换为COE格式
        coe_data = ""
        for i in range(0, len(data), 2):
            coe_data += data[i:i+2] + " "
        coe_content += coe_data.strip() + "\n"

# 将COE内容写入文件
coe_file = open("output.coe", "w")
coe_file.write(coe_content)
coe_file.close()