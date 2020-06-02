package com.shc.test.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author haichao.shao
 * @since 2020-06-02 10:37
 */
public class ReadFile {
    public static void main(String[] args) throws IOException {
        List<Integer> stringList = Arrays.asList(39957152, 39949628, 43777018, 15941348, 27378199, 15320528, 24727310, 12117244, 7543344, 10995276, 30823767, 23527462, 27379089, 12419670, 5944348, 2433576, 35767608, 24335575, 25816920, 12506504, 26379114, 18504151, 6617192, 12115834, 14385405, 26577508, 17482678, 23209850, 12569670, 11972242, 15367049, 15526823, 25196643, 9701181, 24374562, 18042693, 23659440, 5837327, 20133218, 12119185, 26105070, 20955526, 25185764, 7819384, 12378149, 23680808, 23172155, 27430527, 23262272, 14149987, 12285025, 31100791, 13141943, 24182898, 22631685, 26492750, 6816923, 12956401, 10359199, 36532010, 9632291, 19762087, 5099227, 27378405, 15229155, 36553585, 19741649, 12156504, 12635170, 34426229, 19897230, 20148847, 10653196, 13211639, 23784012, 12487122, 6941537, 22799450, 8154812, 1485574, 23165574, 3720918, 27809684, 24966150, 23243087, 27429617, 21025779, 25346575, 14574046, 27512909, 11662199, 5315772, 25269847, 9828331, 9358330, 5579950, 15430811, 25008080, 9086620, 26488664, 8724173, 13932428, 28464813, 3811074, 27466670, 12175877, 26589538, 12346196, 26496447, 21180161, 1772301, 11471324, 17009229, 20390166, 15651593, 3881591, 27975252, 4496932, 9467282, 27688020);
        List<Long> lista = new ArrayList<>();
        for (Integer aa : stringList) {
            lista.add(aa.longValue());
        }
        System.out.println(stringList.size());
        List<Long> listb = readFile();
        lista.removeAll(listb);
        System.out.println(lista);
        System.out.println(lista.size());
    }

    private static List readFile() throws IOException {
        String fileName = "/Users/haichao.shao/work/已发送短信.txt";

        FileReader fileReader = new FileReader(fileName);

        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line = bufferedReader.readLine();
        List list = new ArrayList<>();
        while (line != null) {
            list.add(Long.parseLong(line));
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        fileReader.close();
        return list;
    }


}
